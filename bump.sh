perl -p -i -e 's/(\d+)/1 + /ge' gradle.properties
git add gradle.properties
git commit -m "Bumping counter"
