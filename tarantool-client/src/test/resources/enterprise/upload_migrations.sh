#!/bin/bash

echo "---" > ./to_load.yml

for filename in $(find migrations -name '*.lua')
do
  echo $filename ": |-" >> ./to_load.yml
  cat $filename | sed 's/^/  /' >> ./to_load.yml
done

echo "..." >> ./to_load.yml

curl 'http://localhost:8081/admin/config' --upload-file to_load.yml
rm ./to_load.yml
