def readme = new File(context.projectDir, "README.md")

assert readme.exists() : "README.md not generated at ${readme}"
assert readme.text.contains("# my-box-app") : "artifactId not substituted"
assert !readme.text.contains('${') : "Unresolved variables found"
