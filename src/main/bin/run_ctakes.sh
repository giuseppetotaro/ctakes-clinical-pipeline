#!/bin/bash
#
# Script     : run_ctakes.sh
# Usage      : ./run_ctakes.sh -i /path/to/input -o /path/to/output -u user -p password [-c /path/to/ctakes]
# Author     : Giuseppe Totaro
# Date       : 08-06-2015 [MM-DD-YYYY]
# Last Edited: 08-06-2015, Giuseppe Totaro
# Description: This scripts relies on Apache cTAKES to run the complete pipeline
#              for annotating clinical documents in plain text format using the 
#              built in UMLS (SNOMEDCT and RxNORM) dictionaries. 
# Notes      : You need the UMLS credentials to run this program. To apply for 
#              UMLS credentials, please visit this link:
#              https://uts.nlm.nih.gov/home.html
#

function usage() {
        echo "Usage: run_ctakes.sh -i /path/to/input -o /path/to/output -u user -p password [-j /path/to/jar] [-c /path/to/ctakes]"
        exit 1
}

INPUT=""
OUTPUT=""
UMLS_USER=""
UMLS_PASS=""
CTAKES_HOME=""
CLINICAL_PIPELINE_JAR="../../../target/ctakes-clinical-pipeline-0.0.1-SNAPSHOT.jar"

while [ "$1" != ""  ] 
do
        case $1 in
                -i|--input)
                INPUT="$2"
                shift
                ;;  
                -o|--output)
                OUTPUT="$2"
                shift
                ;;  
                -u|--user)
                UMLS_USER=$2
                shift
                ;;  
                -p|--password)
                UMLS_PASS=$2
                shift
                ;;
		-c|--ctakes-home)
		CTAKES_HOME=$2
                shift
		;;
		-j|--jar)
		CLINICAL_PIPELINE_JAR=$2
		shift
		;;
                *)  
                usage
                ;;  
        esac
	shift
done

if [ "$INPUT" == "" ] || [ "$OUTPUT" == "" ] || [ "$UMLS_USER" == "" ] || [ "$UMLS_PASS" == "" ]
then
        usage
fi

[[ $CTAKES_HOME == "" ]] && CTAKES_HOME=/usr/local/apache-ctakes-3.2.2

if [ ! -d ${CTAKES_HOME}/lib ]
then
	echo "Error: ${CTAKES_HOME}/lib does not exist!"
	exit 1
fi

if [ ! -f $CLINICAL_PIPELINE_JAR ]
then
	echo "Error: $CTAKES_PIPELINE_JAR does not exist. Please provide the jar that contains CTAKESClinicalPipelineFactory class."
	exit 1
fi

#JARS=($(find ${CTAKES_HOME}/lib -iname "*.jar" -type f))
#SAVE_IFS=$IFS
#IFS=$":"
#JOIN="${JARS[*]}"
#IFS=$SAVE_IFS

CTAKES_JARS=""
for jar in $(find ${CTAKES_HOME}/lib -iname "*.jar" -type f)
do
	CTAKES_JARS+=$jar
	CTAKES_JARS+=":"
done

current_dir=$PWD
cd $CTAKES_HOME

#java -Dctakes.umlsuser=${UMLS_USER} -Dctakes.umlspw=${UMLS_PASS} -cp $CTAKES_HOME/desc/:$CTAKES_HOME/resources/:$JOIN -Dlog4j.configuration=file:$CTAKES_HOME/config/log4j.xml -Xms512M -Xmx3g org.apache.ctakes.clinicalpipeline.runtime.BagOfCUIsGenerator ${INPUT} ${OUTPUT}

java -Dctakes.umlsuser=${UMLS_USER} -Dctakes.umlspw=${UMLS_PASS} -cp ${CTAKES_HOME}/desc/:${CTAKES_HOME}/resources/:${CTAKES_JARS%?}:${current_dir}/${CLINICAL_PIPELINE_JAR} -Dlog4j.configuration=file:${CTAKES_HOME}/config/log4j.xml -Xms512M -Xmx3g it.cnr.iac.CTAKESClinicalPipelineFactory ${current_dir}/$INPUT ${current_dir}/$OUTPUT

cd $current_dir

echo "Process completed. Output: ${OUTPUT}"
