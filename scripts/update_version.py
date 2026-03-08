import re
import sys

new_version = sys.argv[1]

with open("pyproject.toml", "r") as f:
    content = f.read()


def replace_project_version(match):
    return f'{match.group(1)}version = "{new_version}"'


pattern = r'(\[tool\.poetry\]\s*.*?)(version\s*=\s*["\'][^"\']+["\'])'
updated = re.sub(pattern, replace_project_version, content, flags=re.DOTALL)

with open("pyproject.toml", "w") as f:
    f.write(updated)

with open("version.txt", "w") as f:
    f.write(new_version + "\n")
