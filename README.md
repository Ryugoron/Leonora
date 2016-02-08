Leonora
=======
*A higher-order problem preprocessor*

Problem normalization has always been an integral part of automated theorem proving (ATP).
Whereas early ATP systems relied heavily on external normalization and clausification,
the normalization of a given input problem has gradually been merged into the prover themselves.
The influence and success of FLOTTER underlined
the importance of careful employment of pre-processing techniques.
Current state-of-the-art first-order ATP systems can spend a large portion of its execution time
on pre-processing. Higher-order (HO) ATPs have not yet developed as sophisticated methods as
their first-order counterparts and use hardly any sophisticated pre-processing techniques regarding clausification.

Leonora incorporates adaptations of prominent first-order techniques that
improve clause normal form (CNF) calculations (cf. Nonnengart et al.)
first analyzed by Kim Kern in the context of higher-order logic (HOL).
These adaptions are further augmented with HOL specific techniques and bundled in
different normalization procedures. These procedures are intended as pre-processing
routines for the new Leo-III theorem prover.


**IMPORTANT:** Due to the very inefficient problem parsing mechanism, the stand-alone tool will take some time to process the input problem as it becomes larger. It is highly recommended to include the normalization libraries directly into the target tool.

Required Dependencies
----------------

Leo III needs Java >= 1.7 to run.
Scala 2.11.6 is required to build and run the project. SBT (Scala build tool) is used to manage
the building process. It will automatically download scala and further dependencies.
Alternative, Scala can be downloaded at [Scala-lang.org](http://scala-lang.org/download/).

Building the project
----------------
The project is compiled using

    > sbt compile
    
Running Leonora
----------------

Leonora can be executed using sbt, i.e. via
    
    > sbt "run problem ARG1 ARG2 ..."

where 'problem' is the input problem file and ARGi are additional arguments to Leonora.
Alternatively, upon invocation of `sbt run`, the program files will be bundled in a .jar file
that can be used to execute Leonora without using sbt.

Command-line arguments
----------------

| Argument | Description |
| --- | --- |
| -e | Full extensional handeling for rewrite rules. |
| -a | 	Enables argument extraction. |
| -s | Enables simplification |
| -p | Translates the problem into a prenex normal form. |
| --rdelta 	|	Offset for the renaming to be triggered. |
| --def |	Handles definitions as unit equations. |


