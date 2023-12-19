#!/bin/sh
for f in *.pnml; do
	echo Running ${f} ...
	java -jar DPNRepair-fat-1.0-SNAPSHOT.jar ${f}
done
