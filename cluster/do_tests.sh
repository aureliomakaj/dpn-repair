#!/bin/sh
for f in acyclic/*.pnml; do
	echo Running ${f} ...
	java -Xmx32G -jar DPNRepair-fat-1.0-SNAPSHOT.jar --file-path ${f} 
done

for f in cyclic/*.pnml; do
	echo Running ${f} ...
	java -Xmx32G -jar DPNRepair-fat-1.0-SNAPSHOT.jar --cyclic --file-path ${f} 
done
