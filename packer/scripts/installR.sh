apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9
add-apt-repository 'deb [arch=amd64,i386] https://cran.rstudio.com/bin/linux/ubuntu xenial/'
apt-get update
apt-get install -y r-base r-base-dev

# Install R Studio
cd /tmp
wget -c https://download1.rstudio.org/rstudio-xenial-1.0.153-amd64.deb
dpkg -i rstudio*.deb
apt-get -f -y install
