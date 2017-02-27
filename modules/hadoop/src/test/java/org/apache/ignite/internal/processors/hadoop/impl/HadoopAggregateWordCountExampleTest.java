package org.apache.ignite.internal.processors.hadoop.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.AggregateWordCount;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.aggregate.ValueAggregatorJob;
import org.apache.hadoop.util.Tool;

/**
 *
 */
public class HadoopAggregateWordCountExampleTest extends HadoopGenericExampleTest {
    /** */
    final Tool tool = new Tool() {
        private Configuration conf;

        @Override public void setConf(Configuration conf) {
            this.conf = conf;
        }

        @Override public Configuration getConf() {
            return conf;
        }

        @SuppressWarnings("unchecked")
        @Override public int run(String[] args) throws Exception {
//            final Configuration conf0 = //new Configuration();
//                ValueAggregatorJob.setAggregatorDescriptors(new Class[] {
//                    AggregateWordCount.WordCountPlugInClass.class});
//
//            final Configuration conf = getConf();
//
//            conf0.addResource(conf);
//
//            String[] otherArgs = new GenericOptionsParser(conf0, args).getRemainingArgs();
//
////            HadoopGenericExampleTest.setAggregatorDescriptors(conf0,
////                new Class[] {
////                    AggregateWordCount.WordCountPlugInClass.class
////                    //ValueAggregatorBaseDescriptor.class
////                    //AggregateWordHistogram.AggregateWordHistogramPlugin.class
////                } );
//
//            Job job = ValueAggregatorJob.createValueAggregatorJob(conf0, otherArgs);

            // Original example code looks like the following:
            // ----------------
            //     Job job = ValueAggregatorJob.createValueAggregatorJob(args, new Class[] {WordCountPlugInClass.class});
            //     job.setJarByClass(AggregateWordCount.class);
            //     int ret = job.waitForCompletion(true) ? 0 : 1;
            // ----------------

            final Configuration conf = getConf();

            setAggregatorDescriptors_WRONG(conf, new Class[] {AggregateWordCount.WordCountPlugInClass.class});

            Job job = ValueAggregatorJob.createValueAggregatorJob(conf, args);

//            Job job = ValueAggregatorJob.createValueAggregatorJob(args
//                , new Class[] {AggregateWordCount.WordCountPlugInClass.class});

            job.setJarByClass(AggregateWordCount.class);

            return job.waitForCompletion(true) ? 0 : 1;
        }
    };

    /** */
    private final GenericHadoopExample ex = new GenericHadoopExample() {
        @Override void prepare(JobConf conf, FrameworkParameters params) throws IOException {
            //generateTextInput(11, conf, params);
        }

        @Override String[] parameters(FrameworkParameters fp) {
//            System.out.println("usage: inputDirs outDir "
//                + "[numOfReducer [textinputformat|seq [specfile [jobName]]]]");
            return new String[] {
                //inDir(fp),
                "/home/ivan/_git/apache-ignite/INPUT",
                outDir(fp),
                "2", // Numper of reduces other than 1 does not make sense, but QA test uses 2 for some reason.
                "textinputformat"
              };
        }

        @Override Tool tool() {
            return tool;
        }

        @Override void verify(String[] parameters) throws Exception {
            Path path = new Path(parameters[1] + "/part-r-00000");

            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getFileSystem().open(path)))) {
                int wc = 0;
                String line = null;

                while (true) {
                    String line0 = br.readLine();

                    if (line0 == null)
                        break;

                    line = line0;

                    wc++;

                    if (wc == 1)
                        assertEquals("record_count\t1000000", line); // first line
                }

                //assertEquals("zoonitic\t22", line); // last line
                assertEquals(1, wc);
            }
        }
    };

    /** {@inheritDoc} */
    @Override protected GenericHadoopExample example() {
        return ex;
    }

//    @Override protected FrameworkParameters frameworkParameters() {
//        return super.frameworkParameters();
//    }

//    /** {@inheritDoc} */
//    @Override protected int numMaps() {
//        return gridCount() * 50;
//    }

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        // Reproduce
        return 1; //super.gridCount();
    }

    /** {@inheritDoc} */
    @Override protected boolean isOneJvm() {
        return true;
    }
}
