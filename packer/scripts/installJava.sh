cd /tmp
wget -c --header "Cookie: oraclelicense=accept-securebackup-cookie" $JDK_DOWNLOAD_URL
tar -xf jdk*
mkdir -p /usr/lib/jvm
mv jdk1.8* java-8-oracle
mv java* /usr/lib/jvm
update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-8-oracle/jre/bin/java 1091
update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-8-oracle/bin/javac 1091
cp $USER_HOME_DIR/uploads/jdk.sh /etc/profile.d/jdk.sh
source /etc/profile.d/jdk.sh
