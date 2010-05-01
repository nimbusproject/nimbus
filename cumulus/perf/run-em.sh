#!/bin/sh

./run-disk.sh | tee d2$1.txt
./run-m2m.sh | tee m2$1.txt
