#!/bin/sh

echo > out.txt
for f in acyclic/*.pnml; do
	echo Running ${f} ... >> out.txt
	java -Xmx32G -jar DPNRepair-fat-1.0-SNAPSHOT.jar --file-path ${f} >> out.txt 
done

for f in cyclic/*.pnml; do
	echo Running ${f} ... >> out.txt
	java -Xmx32G -jar DPNRepair-fat-1.0-SNAPSHOT.jar --cyclic --file-path ${f} >> out.txt
done
