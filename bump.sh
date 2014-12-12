#perl -p -i -e 's/(\d+)/1 + \1 /ge' gradle.properties
perl -p -i -e 's/(counter=)(\d+)/$1.($2+1)/ge' gradle.properties
git add gradle.properties
git commit -m "Bumping counter"
