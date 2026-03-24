# save as: run.ps1  (in the root of HistogramEQ/)

$srcFiles = Get-ChildItem -Recurse -Filter "*.java" src\ | ForEach-Object { $_.FullName }
javac -cp "lib\*" -d out $srcFiles
java -cp "out;lib\*" Main