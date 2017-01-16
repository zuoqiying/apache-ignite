package org.apache.ignite.loadtest;

import com.google.common.cache.CacheBuilder;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.log4j.Logger;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

/**
 * Менеджер идентификаторов dpl-сущностей. Все последовательности идентификаторов хранятся в отдельном КЭШ с отражением в файл
 * Все последовательности хранятся в системном КЭШ с наименованием "DPLEntityIdSequences"
 * Ключом для хранения последовательности для конкретной сущности является наименование последовательности.
 * Наименование последовательности формируется из конкатенации наименования хранимой сущности и постфикса "_$S"
 * При инициализации в последовательность устанавливает начальное значение из КЭШ, если значения в КЭШ было
 * Сохранение в КЭШ происходит лишь в тот момент, когда значение последовательности превышает очередную порцию резервируемых значений
 */
public class DPLEntityIdManager {

    private Logger logger = Logger.getLogger(DPLEntityIdManager.class);
    private String name;
    private int sequenceReserveSize = 16;
    private Ignite ignite;
    private IgniteAtomicSequence sequence;
    private IgniteCache<String, Long> sequenceCache;

    /**
     * CacheEntryProcessor, обеспечивающий "шаг" перед созданием сиквенса, при необходимости
     */
    static public class SequenceInitializationProcessor implements CacheEntryProcessor<String, Long, Long> {

        @Override
        public Long process(MutableEntry<String, Long> mutableEntry, Object... objects) throws EntryProcessorException {
            long initial = (long) objects[0];
            long step = (long) objects[1];
            Long value = mutableEntry.getValue();
            if (value == null) {
                value = initial + step;
                mutableEntry.setValue(value);
                return initial;
            } else {
                value += step;
                mutableEntry.setValue(value);
                return value;
            }
        }
    }

    private static IgniteCache getSequenceCache(Ignite ignite, String name) {
        CacheConfiguration ccfg = new CacheConfiguration();
        ccfg.setName(name);
        ccfg.setCacheMode(CacheMode.REPLICATED);
        ccfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        return ignite.getOrCreateCache(ccfg);
    }

    /**
     * Инициализация последовательности
     *
     * @param name            наименование последовательности
     * @param startValue      начальное значение КЭШа
     */
    public DPLEntityIdManager(Ignite ignite, String name, Long startValue) {
        this.ignite = ignite;

        this.name = name + "_$S";

        if (logger.isTraceEnabled())
            logger.trace("Инициализация последовательности с наименованием " + name);

        //Создаём ХЭШ-таблицу
        sequenceCache = getSequenceCache(ignite, "sequence-cache-" + name);

        // Пытаемся получить сиквенс
        try {
            sequence = ignite.atomicSequence(this.name, 0, false);
        }
        catch (IgniteException ignore) {
        }
        if (sequence == null) {
            // Если сиквенса в гриде еще нету, пытаемся создать сиквенс с учетом того, что кто-то в это-же время
            // может делать то-же самое
            long init = startValue - 1;
            long step = ignite.cluster().nodes().size() * sequenceReserveSize;
            // TODO следующая строка - workaround бага https://gridgain.freshdesk.com/support/tickets/1667
            // нужно пересмотреть после фикса
            sequenceCache.get(this.name);

            Long sequenceInitialValue = sequenceCache.invoke(this.name, new SequenceInitializationProcessor(), init, step);
            sequence = ignite.atomicSequence(this.name, sequenceInitialValue, true);
        }

        if (logger.isTraceEnabled())
            logger.trace("Инициализация последовательности с наименованием " + name() + " завершена");
    }

    /**
     * Получить следующее значение из последовательности. Если значение кратно размеру резервирования идентификаторов,
     * то в КЭШ записываем начало новой корзины. Это необходимо для того, чтобы следующее чтение (инициализация) последовательности
     * началось со следующей корзины. Это исключит дублирование идентификаторов
     *
     * @return следующее значение из последовательности
     */
    public long nextValue() {
        long nextValue = sequence.incrementAndGet();

        // Пропускаем 0 в последовательности
        if (nextValue == 0L) {
            nextValue = sequence.incrementAndGet();
        }

        if (nextValue % sequenceReserveSize == 0) {
            if (logger.isTraceEnabled())
                logger.trace("Сохранение текущего значения " + nextValue + " последовательнсоти " + name);

            // Сохранение в КЭШ начала следующей корзины идентификаторов. Каждая корзина начинается с
            // Long.MIN_VALUE или числу кратному ему, если не было установлено стартовое значение
            synchronizeWithSequenceCache(nextValue);
        }

        return nextValue;
    }

    /**
     * Синхронизовать последнее полученное значение последовательности для данной сущности
     * Так как в момент синхронизации обращения к данной последовательности могут осущетсвляться из других потоков (в том числе работающих на других узлах),
     * то необходимо учитывать, что при использовании ATOMIC режима работы хэш-таблицы необходимо обеспечивать согласованность синхронизации алгоритмически
     *
     * В данном случае может иметь ситуация, когда другой поток осуществил синхронизацию с последовательностью вперёд текущего.
     * Из этой ситуации есть два крайних случая - более быстрый поток отставал по значению последовательности относительно текущего, либо обгонял.
     * В случае если он обгонял, то согласованность данных не нарушена синхронизацию можно не осуществлять
     * В противном случае, необходимо повторить операцию синхронизацию до тех пор, пока сохранённое значение не будет консистентно
     *
     * @param lastValue последнее полученное значение sequence-а
     */
    private void synchronizeWithSequenceCache(long lastValue){
        //Выполняем попытку синхронизации либо до успешного выполнения, либо до момента когда это обновление перестанет быть актуальным (сохраненённое значение
        //ушло вперёд посредством деятельности другого потока
        while(true) {
            //Получить текущее сохранённое значение последовательности
            long actualReserveValue = sequenceCache.get(name());
            //Расчитать следующее значение окна
            long nextReserveValue = lastValue + sequenceReserveSize + 1;
            //Если другой поток уже обновил значение последовательности на равное заданному или большее, то данную операцию завершать нет необходимости
            if (actualReserveValue >= nextReserveValue) {
                break;
            }
            //Осуществляем попытку синхронизации предыдущего значения окна при условии, что предыдущее значение равно actualReserveValue
            if(sequenceCache.replace(name(), actualReserveValue, nextReserveValue)){
               //Если синхронизация прошла успешно, то выходим
               break;
            }

            //В противном случае это означает, что другой поток (возможно в другом узле) уже провёл синхронизацию значения
            //и необходимо выполнить попытку синхронизации повторно
            try {
                //В случае конкуретного доступа имеет смысл ввести случайную задержку, чтобы минимизировать вероятность конкуретного доступа на следующей итерации
                //Чтобы не овеличивать overhead на сдвиг, задержка формируется в нано разрядах функции Thread.sleep
                //Данный фрагмент особенно актуален при конкуретном доступе из различных потоков расположенных на одном узле
                Thread.sleep(1, (int)(Math.random()*1000));
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Получить наименование поледовательности
     *
     * @return наименование поледовательности
     */
    public String name() {
        return name;
    }

    public void destroySequence() {
        ignite.destroyCache(name);
        sequence.close();
    }
}
