# ctakes-clinical-pipeline
A command line tool based on [Apache cTAKES](http://ctakes.apache.org/) in order to run the complete pipeline for annotating clinical documents in plain test format using the built in UMLS (SNOMEDCT and RxNORM) dictionaries. 

Basically, this tool extends the ClinicalPipelineFactory class by adding the opportunity to run the cTAKES pipeline on arbitrary text instead of using only the static text defined into ClinicalPipelineFactory.java.

## Getting started

> bin/run_ctakes.sh -i /path/to/input /path/to/output -o -u umls_user -p umls_pass [-c /path/to/ctakes]

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
