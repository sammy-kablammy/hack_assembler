#!/bin/bash

# A simple script to run the test cases in the 'testing' directory.
# It will use the given command to assemble each file, comparing it to the
# .cmp file of the same name

if [ $# -ne 1 ]; then
    echo "Usage: $0 \"<assembler_command>\""
    echo "Make sure your assembler program needs to accept a .asm file as its" \
    "first command-line argument."
    exit 1
fi

assembler_command="$1"

for asm_file in *.asm; do
    # note: '-f' checks if a file is a normal file as opposed to a directory
    if [ -f "$asm_file" ]; then
        base_name=$(basename "$asm_file" .asm)

        # do the assembling
        echo "Now assembling $asm_file..."
        $assembler_command $asm_file

        # note: 'diff -q' outputs only when files differ.
        # note: the nand2tetris provided files might have windows-style line
        # endings. use '--strip-trailing-cr' to remove this.
        diff_result=$(diff --strip-trailing-cr -q "$base_name.hack" "$base_name.cmp")
        if [ -z "$diff_result" ]; then # note: '-z' checks for zero length
            echo "    $asm_file assembled successfully! :)"
        else
            echo "Uhhh... $asm_file does not match the compare file :("
        fi
    else
        echo "Uhhh... $asm_file is not a valid .asm file :("
    fi
done
