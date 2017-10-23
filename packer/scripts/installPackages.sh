#update and upgrade ubuntu
apt-get update
apt-get -y upgrade

#generic ubuntu packages
apt-get install -y git maven python-pip parallel curl htop iotop jq pcregrep zsh python3-pip

#python 2.7 packages
pip install numpy scipy matplotlib pandas nltk regex GenSim ujson dask toolz theano csvkit htrc-feature-reader

#python 3 packages
pip3 install numpy scipy matplotlib pandas nltk regex GenSim ujson dask toolz theano csvkit htrc-feature-reader

#fixing the terminal LANG issue
locale-gen
localectl set-locale LANG="en_US.UTF-8"
