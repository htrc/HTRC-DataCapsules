beginswith () { case $2 in "$1"*) true;; *) false;; esac; }

endswith () { case $2 in *"$1") true;; *) false;; esac; }
