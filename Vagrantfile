# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network "forwarded_port", guest: 22, host: 2230, id: "ssh"

  config.vm.synced_folder "..", "/home/vagrant/work/stapleslabs"
  config.vm.synced_folder ".", "/vagrant", disabled: true

  config.vm.provider "virtualbox" do |vb|
    vb.cpus = 2
    vb.memory = "4096"
  end

  config.vm.provision "shell", inline: <<-SHELL
    su - vagrant -c "cd /home/vagrant/Wonko && bash bin/setup.sh"
  SHELL
end
