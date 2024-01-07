# Data Petri Net Repair

This project is the concrete implementation of my thesis 
"Data-aware soundness verification and repair of Data Petri-Nets".

It is composed of mainly three parts: parsing, soundness verification, repair.

### Parsing
All the files in the directory **"parser"** are used for representing
a Data Petri Net, parsed from a .pnml file (Petri Net Model Language).

The flow and programs I used for the generation of a Data Petri Net file is:
- Create a classic data-less Petri Net using "Yasper" program (https://www.yasper.org/)
- Using ProM (https://promtools.org/), load the Petri Net previously created and, 
  using the "Create/Edit PetriNet With Data", add the guards to the transitions. ATTENTION: only Long types are supported
- The output is a little different from the parser, thus manually do this changes:
  1. Add the "\<initialMarkings\>" tag (just duplicate "finalMarkings" and set the tokens to the initial places)
  2. [Optional] For each "\<variable\>" tag, add the attribute "initialvalue". If not present, it will be set to 0.

### Soundness verification
Soundness verification is performed when creating an instance of ConstraintGraph class. 
This class create the constraint graph of the Data Petri Net and verifies that it has not dead nodes and no
missing transitions. 

### Repair
There are two different classes for the repair. DPNRepairAcyclic is for acyclic Data Petri Nets while DPNRepairCyclic is 
for cyclic DPNs. 
Both classes have a public repair() method, but for DPNRepairCyclic the constructor requires a
SolverContext class (creatable by using SmtSolverFactory class), used for an addition
soundness verification for cyclic cases (co-reachability analysis). 


## Usage
Once you create the jar, there are two possible parameters:
1. --file-path "my\dpn\path" : put the full path of the DPN file to parse
2. --cyclic: tells the program that the file is a cyclic DPN (false by default)
3. --no-output-files: tells the program to NOT write the output files

When the execution terminates, in the same folder of the input file, it will
create three files, named using the name of the input file with the following suffixes: 
1. my-file-cg-raw.txt, containing the nodes of the constraint graph
2. my-file-repaired-cg-raw.txt, containing the nodes of the constraint graph of the REPAIRED DPN
3. my-file-repaired-cg.xml, containing the constraint graph in an GraphML format


