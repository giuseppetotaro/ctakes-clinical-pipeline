# ctakes-clinical-pipeline
A command line tool based on [Apache cTAKES](http://ctakes.apache.org/) in order to run the complete pipeline for annotating clinical documents in plain text format using the built in UMLS (SNOMEDCT and RxNORM) dictionaries. 

Basically, this tool extends the ClinicalPipelineFactory class by adding the opportunity to run the cTAKES pipeline on arbitrary text instead of using only the static text defined into ClinicalPipelineFactory.java.

## Getting started

To build the project, you can type the following command:

> mvn clean install

To run the ctakes-clinical-pipeline, you can launch the following script (in `/src/main/bin`):

> ./run_ctakes.sh -i /path/to/input /path/to/output -o -u umls_user -p umls_pass [-j /path/to/jar] [-c /path/to/ctakes]

Please be sure that Apache cTAKES (both jar files and resources) is correctly installed on your machine.

## Apache cTAKES

[Apache cTAKES](http://ctakes.apache.org/) (clinical Text Analysis and Knowledge Extraction System) is an open-source natural language processing system for information extraction from electronic medical record clinical free-text. It processes clinical notes identifying types of clinical named entities.

cTAKES has been built using the [Apache UIMA](https://uima.apache.org/) framework and [OpenNLP](https://opennlp.apache.org/) toolkit. Its components are specifically trained for the clinical domain, and create rich linguistic and semantic annotations that can be utilized by clinical decision support systems and clinical research.

Apache cTAKES can be installed and configured following the instructions provided by the [User Installation guide](https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+3.2+User+Install+Guide).

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
