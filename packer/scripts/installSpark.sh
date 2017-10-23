cd /tmp
wget -c $SPARK_DOWNLOAD_URL 
mkdir spark && tar xf spark*.tgz -C spark --strip-components 1
mv spark /opt/spark

cp /home/dcuser/uploads/spark.sh /etc/profile.d/spark.sh
source /etc/profile.d/spark.sh

