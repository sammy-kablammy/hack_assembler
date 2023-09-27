# Hack assembler

Following "From NAND to Tetris" course / "The Elements of Computing Systems"
book. This part is from week ~6.

[https://www.nand2tetris.org/](https://www.nand2tetris.org/)

## Testing
There is a test script in the 'testing' directory.
Give it the command used to run your assembler and it will run a small test
suite.

For example, to run the "Assembler" java file located one directory above the
'testing' directory, use the following command:

```bash
hack_assembler/testing$ ./testing.sh "java -cp ../java Assembler"
```

Your assembler program should accept the name of a .asm file as its first (and
only) command line argument.
