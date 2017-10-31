# Adds the htrcVirt user
adduser --disabled-password --gecos "" $HTRCVIRT_USERNAME
usermod -aG sudo $HTRCVIRT_USERNAME

# Add the required public key - this can be replaced at ../scripts/authorized_keys file
mkdir /home/$HTRCVIRT_USERNAME/.ssh
cp /home/$SSH_USERNAME/uploads/authorized_keys /home/$HTRCVIRT_USERNAME/.ssh/authorized_keys
chown -R $HTRCVIRT_USERNAME /home/$HTRCVIRT_USERNAME/.ssh
chmod 700 /home/$HTRCVIRT_USERNAME/.ssh
chmod 600 /home/$HTRCVIRT_USERNAMEe.ssh/authorized_keys

# Removing the password for the DCUSER
cp /home/$SSH_USERNAME/uploads/removeSudoPasswords /etc/sudoers.d/removeSudoPasswords
chmod 0440 /etc/sudoers.d/removeSudoPasswords
passwd -d $SSH_USERNAME
