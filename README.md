# Elasticsearch Hadoop [![Build Status](http://build.elasticsearch.com/job/es-hadoop/badge/icon)](http://build.elasticsearch.com/view/Hadoop/) [![Build Status](https://travis-ci.org/elasticsearch/elasticsearch-hadoop.png)](https://travis-ci.org/elasticsearch/elasticsearch-hadoop)
Elasticsearch real-time search and analytics natively integrated with Hadoop. Supports [MapReduce](#mapreduce), [Cascading](#cascading), [Hive](#hive) and [Pig](#pig).

See the official [project page](http://www.elasticsearch.org/overview/hadoop/) and [documentation](http://www.elasticsearch.org/guide/en/elasticsearch/hadoop/current/index.html) for more, in-depth information.

# Requirements
Elasticsearch (version __0.90__ or higher) cluster accessible through [REST][]. That's it!
Significant effort has been invested to create a small, dependency-free, self-contained jar that can be downloaded and put to use without any dependencies. Simply make it available to your job classpath and you're set.
For a certain library, see the dedicated [chapter](http://www.elasticsearch.org/guide/en/elasticsearch/hadoop/current/requirements.html).

# Installation

## Release 1.3.0 M1
Available through any Maven-compatible tool:

```xml
<dependency>
  <groupId>org.elasticsearch</groupId>
  <artifactId>elasticsearch-hadoop</artifactId>
  <version>1.3.0.M1</version>
</dependency>
```
or as a stand-alone [ZIP](https://download.elasticsearch.org/hadoop/hadoop-latest.zip).

## Development Snapshot
Grab the latest nightly build from the [repository](http://oss.sonatype.org/content/repositories/snapshots/org/elasticsearch/elasticsearch-hadoop/) again through Maven:

```xml
<dependency>
  <groupId>org.elasticsearch</groupId>
  <artifactId>elasticsearch-hadoop</artifactId>
  <version>1.3.0.BUILD-SNAPSHOT</version>
</dependency>
```

```xml
<repositories>
  <repository>
    <id>sonatype-oss</id>
    <url>http://oss.sonatype.org/content/repositories/snapshots</url>
	<snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>
```

or [build](#building-the-source) the project yourself. 

We do build and test the code on _each_ commit; see our CI server [![here](http://build.elasticsearch.com/job/es-hadoop/badge/icon)](http://build.elasticsearch.com/view/Hadoop/) 

## Hadoop 2.0/YARN build

We also publish a YARN-based binary (since YARN is not backwards compatible with Hadoop 1.x) - use this when using the Hadoop 2.0/YARN binaries by specifying the `yarn` classifier:

```xml
<dependency>
  <groupId>org.elasticsearch</groupId>
  <artifactId>elasticsearch-hadoop</artifactId>
  <version>1.3.0.BUILD-SNAPSHOT</version>
  <classifier>yarn</classifier>
</dependency>
```

More information in this [section](http://www.elasticsearch.org/guide/en/elasticsearch/hadoop/current/download.html).

# Feedback / Q&A
We're interested in your feedback! You can find us on the User [mailing list](https://groups.google.com/forum/?fromgroups#!forum/elasticsearch) - please append `[Hadoop]` to the post subject to filter it out. For more details, see the [community](http://www.elasticsearch.org/community/) page.

# Usage


## Configuration Properties

All configuration properties start with `es` prefix. Note that the `es.internal` namespace is reserved for the library internal use and should _not_ be used by the user at any point.
The properties are read mainly from the Hadoop configuration but the user can specify (some of) them directly depending on the library used. 

The full list is available [here](http://www.elasticsearch.org/guide/en/elasticsearch/hadoop/current/configuration.html)

### Required
```
es.resource=<ES resource location, relative to the host/port specified above. Can be an index or a query>
```
### Optional
```
es.host=<ES host address> 				       # defaults to localhost
es.port=<ES REST port>    				       # defaults to 9200
es.bulk.size.bytes=<bulk size in bytes>        # defaults to 10mb
es.bulk.size.entries=<bulk size in entries>    # defaults to 0 (meaning it's not set)
es.http.timeout=<timeout for http connections> # defaults to 1m
```

## [MapReduce][]

For basic, low-level or performance-sensitive environments, ES-Hadoop provides dedicated `InputFormat` and `OutputFormat` that read and write data to ElasticSearch. To use them, add the `es-hadoop` jar to your job classpath
(either by bundling the library along - it's ~150kB and there are no-dependencies), using the [DistributedCache][] or by provisioning the cluster manually.

Note that es-hadoop supports both the so-called 'old' and the 'new' API through its `ESInputFormat` and `ESOutputFormat` classes.

### 'Old' (`org.apache.hadoop.mapred`) API

### Reading
To read data from ES, configure the `ESInputFormat` on your job configuration along with the relevant [properties](#configuration-properties):
```java
JobConf conf = new JobConf();
conf.setInputFormat(ESInputFormat.class);
conf.set("es.resource", "radio/artists/_search?q=me*"); // replace this with the relevant query
...
JobClient.runJob(conf);
```
### Writing
Same configuration template can be used for writing but using `ESOuputFormat`:
```java
JobConf conf = new JobConf();
conf.setOutputFormat(ESOutputFormat.class);
conf.set("es.resource", "radio/artists"); // index or indices used for storing data
...
JobClient.runJob(conf);
```
### 'New' (`org.apache.hadoop.mapreduce`) API

### Reading
```java
Configuration conf = new Configuration();
conf.set("es.resource", "radio/artists/_search?q=me*"); // replace this with the relevant query
Job job = new Job(conf)
job.setInputFormat(ESInputFormat.class);
...
job.waitForCompletion(true);
```
### Writing
```java
Configuration conf = new Configuration();
conf.set("es.resource", "radio/artists"); // index or indices used for storing data
Job job = new Job(conf)
job.setOutputFormat(ESOutputFormat.class);
...
job.waitForCompletion(true);
```
## [Hive][]
ES-Hadoop provides a Hive storage handler for ElasticSearch, meaning one can define an [external table][] on top of ES.

Add es-hadoop-<version>.jar to `hive.aux.jars.path` or register it manually in your Hive script (recommended):
```
ADD JAR /path_to_jar/es-hadoop-<version>.jar;
```
### Reading
To read data from ES, define a table backed by the desired index:
```SQL
CREATE EXTERNAL TABLE artists (
    id      BIGINT,
    name    STRING,
    links   STRUCT<url:STRING, picture:STRING>)
STORED BY 'org.elasticsearch.hadoop.hive.ESStorageHandler'
TBLPROPERTIES('es.resource' = 'radio/artists/_search?q=me*');
```
The fields defined in the table are mapped to the JSON when communicating with ElasticSearch. Notice the use of `TBLPROPERTIES` to define the location, that is the query used for reading from this table:
```
SELECT * FROM artists;
```

### Writing
To write data, a similar definition is used but with a different `es.resource`:
```SQL
CREATE EXTERNAL TABLE artists (
    id      BIGINT,
    name    STRING,
    links   STRUCT<url:STRING, picture:STRING>)
STORED BY 'org.elasticsearch.hadoop.hive.ESStorageHandler'
TBLPROPERTIES('es.resource' = 'radio/artists/');
```

Any data passed to the table is then passed down to ElasticSearch; for example considering a table `s`, mapped to a TSV/CSV file, one can index it to ElasticSearch like this:
```SQL
INSERT OVERWRITE TABLE artists 
    SELECT NULL, s.name, named_struct('url', s.url, 'picture', s.picture) FROM source s;
```

As one can note, currently the reading and writing are treated separately but we're working on unifying the two and automatically translating [HiveQL][] to ElasticSearch queries.

## [Pig][]
ES-Hadoop provides both read and write functions for Pig so you can access ElasticSearch from Pig scripts.

Register ES-Hadoop jar into your script or add it to your Pig classpath:
```
REGISTER /path_to_jar/es-hadoop-<version>.jar;
```
Additionally one can define an alias to save some chars:
```
%define ESSTORAGE org.elasticsearch.hadoop.pig.ESStorage()
```
and use `$ESSTORAGE` for storage definition.

### Reading
To read data from ES, use `ESStorage` and specify the query through the `LOAD` function:
```
A = LOAD 'radio/artists/_search?q=me*' USING org.elasticsearch.hadoop.pig.ESStorage();
DUMP A;
```

### Writing
Use the same `Storage` to write data to ElasticSearch:
```
A = LOAD 'src/test/resources/artists.dat' USING PigStorage() AS (id:long, name, url:chararray, picture: chararray);
B = FOREACH A GENERATE name, TOTUPLE(url, picture) AS links;
STORE B INTO 'radio/artists' USING org.elasticsearch.hadoop.pig.ESStorage();
```

## [Cascading][]
ES-Hadoop offers a dedicate ElasticSearch [Tap][], `ESTap` that can be used both as a sink or a source. Note that `ESTap` can be used in both local (`LocalFlowConnector`) and Hadoop (`HadoopFlowConnector`) flows:

### Reading
```java
Tap in = new ESTap("radio/artists/_search?q=me*");
Tap out = new StdOut(new TextLine());
new LocalFlowConnector().connect(in, out, new Pipe("read-from-ES")).complete();
```
### Writing
```java
Tap in = Lfs(new TextDelimited(new Fields("id", "name", "url", "picture")), "src/test/resources/artists.dat");
Tap out = new ESTap("radio/artists", new Fields("name", "url", "picture"));
new HadoopFlowConnector().connect(in, out, new Pipe("write-to-ES")).complete();
```

# Building the source

ElasticSearch Hadoop uses [Gradle][] for its build system and it is not required to have it installed on your machine. By default, it automatically runs unit tests. For integration testing, one can enable the various test suites through the `enableXXX` tasks (run `gradlew tasks enable` for hints). To enable all integration tests, use `gradlew enableIntegrationTests test build`

To create a distributable jar, run `gradlew -x test build` from the command line; once completed you will find the jar in `build/libs`.

# License
This project is released under version 2.0 of the [Apache License][]

```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2009-2013 ElasticSearch <http://www.elasticsearch.org>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```



[Hadoop]: http://hadoop.apache.org
[MapReduce]: http://hadoop.apache.org/docs/r1.0.4/mapred_tutorial.html
[Pig]: http://pig.apache.org
[Hive]: http://hive.apache.org
[HiveQL]: http://cwiki.apache.org/confluence/display/Hive/LanguageManual
[external table]: http://cwiki.apache.org/Hive/external-tables.html
[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
[Gradle]: http://www.gradle.org/
[REST]: http://www.elasticsearch.org/guide/reference/api/
[DistributedCache]: http://hadoop.apache.org/docs/stable/api/org/apache/hadoop/filecache/DistributedCache.html
[Cascading]: http://www.cascading.org/
[Tap]: http://docs.cascading.org/cascading/2.1/userguide/html/ch03s05.html