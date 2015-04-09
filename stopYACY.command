cd `dirname $0`

# generating the proper classpath
CLASSPATH=""
for N in `ls -1 lib/*.jar`; do CLASSPATH="$CLASSPATH$N:"; done	
for N in `ls -1 libx/*.jar`; do CLASSPATH="$CLASSPATH$N:"; done

java -classpath classes:htroot:$CLASSPATH net.yacy.yacy -shutdown

echo "Please wait until the YaCy daemon process terminates."
echo "You can monitor this with 'tail -f DATA/LOG/yacy00.log' and 'fuser log/yacy00.log'"
