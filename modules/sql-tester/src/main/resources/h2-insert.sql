DROP TABLE IF EXISTS SampleValue;
CREATE TABLE SampleValue(id INT PRIMARY KEY, anotherField VARCHAR(255));
INSERT INTO SampleValue VALUES(1, 'Hello');
INSERT INTO SampleValue VALUES(2, 'World');
SELECT * FROM SampleValue ORDER BY ID;
