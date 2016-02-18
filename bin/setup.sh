#!/usr/bin/env bash

echo "
####################################################################################
# Setup a Linux box for development / testing. Prefer to do this in a shell        #
# script rather than in deps, because a number of deps scripts are OS X specific   #
# (java and lein, particularly), and it seemed easier to do it this way for the    #
# time being.                                                                      #
####################################################################################
"


is_linux() {
    [[ $(uname -a) == *"Linux"* ]]
}

if ! is_linux; then
    echo "The setup script only supports Linux at this point."
    exit 1
fi

# Java
mkdir -p /tmp/${USER}
cd /tmp/${USER}/
curl -L -O -H "Cookie: oraclelicense=accept-securebackup-cookie" -k "https://edelivery.oracle.com/otn-pub/java/jdk/8u74-b02/jdk-8u74-linux-x64.tar.gz"
tar zxvf jdk-8u74-linux-x64.tar.gz
sudo mv jdk1.8.0_74 /usr/local/
sudo touch /usr/local/jdk1.8.0_74 /bin/java
sudo chown -R root /usr/local/jdk1.8.0_74
sudo update-alternatives --remove-all java || true
sudo update-alternatives --install /usr/bin/java java /usr/local/jdk1.8.0_74/bin/java 1;
sudo update-alternatives --set java /usr/local/jdk1.8.0_74/bin/java
sudo update-alternatives --remove-all javac || true
sudo update-alternatives --install /usr/bin/javac javac /usr/local/jdk1.8.0_74/bin/javac 1;
sudo update-alternatives --set javac /usr/local/jdk1.8.0_74/bin/javac
sudo touch /usr/bin/javac
sudo update-alternatives --remove-all jar || true
sudo update-alternatives --install /usr/bin/jar jar /usr/local/jdk1.8.0_74/bin/jar 1;
sudo update-alternatives --set jar /usr/local/jdk1.8.0_74/bin/jar
sudo touch /usr/bin/jar

cd -
sudo apt-get update;
sudo apt-get install -y tmux git make s3cmd htop unzip;
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod a+x lein
sudo mv lein /usr/local/bin/
