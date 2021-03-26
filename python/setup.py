from setuptools import setup, find_packages
from os import path

current_dir = path.dirname(path.realpath(__file__))
src_dir = path.join(current_dir, "src")

setup(
    name='eclipsetcf',
    version='1.0.0',
    url='https://git.eclipse.org/r/tcf/org.eclipse.tcf.git',
    packages=find_packages(src_dir, exclude=["docs"]),
    package_dir={"": src_dir}
)
