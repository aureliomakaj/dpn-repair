#!/bin/bash
#SBATCH --job-name="dpn-repair"
#SBATCH --error=dpn-repair.err
#SBATCH --output=dpn-repair.out
#SBATCH --partition=allgroups
#SBATCH --ntasks=1
#SBATCH --nodes=1
#SBATCH --mem=32G
#SBATCH --time=1-00:00:00
singularity exec dpn-repair.sif ./do_tests.sh 
