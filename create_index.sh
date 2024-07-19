#!/bin/bash $1
 
DIR=$1
 
# Get the list of files in the current directory and its subdirectories
files=$(find ./$DIR -type f)
 
# Create the HTML content for the file listing
html_content="<html><body><ul>"
 
for file in $files; do
    html_content+="<li><a href='$file'>$file</a></li>"
done
html_content+="</ul></body></html>"
 
# Write the HTML content to index.html
echo "$html_content" > index.html
 
# Print a success message
echo "File listing (including subdirectories) has been converted to index.html"
