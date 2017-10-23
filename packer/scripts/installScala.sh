cd /tmp
apt-get remove scala-library scala
wget $SCALA_DOWNLOAD_URL
dpkg -i scala*
wget $SBT_DOWNLOAD_URL
dpkg -i sbt*
