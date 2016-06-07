CC=gcc
CFLAGS=-I.

%.o: %.c 
	$(CC) -c -o $@ $< $(CFLAGS)

tapinit: tapinit.o 
	gcc -o tapinit tapinit.o -I.

.PHONY: clean

clean:
	rm -f *.o *~ euid