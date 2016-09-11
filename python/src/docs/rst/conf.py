import time

extensions = ['sphinx.ext.autodoc', 'sphinx.ext.doctest',
              'sphinx.ext.intersphinx', 'sphinx.ext.todo',
              'sphinx.ext.coverage', 'sphinx.ext.imgmath',
              'sphinx.ext.ifconfig', 'sphinx.ext.viewcode']

templates_path = []
source_suffix = '.rst'
master_doc = 'index'

project = 'TCF Python'

span = '2016'
year = int(time.strftime('%Y'))
if year > 2016:
    span += '-' + str(year)

copyright = span  # @ReservedAssignment

version = '1.5'
release = '1.5'

exclude_patterns = ['_build', 'globals.rst']

html_theme = 'default'
html_static_path = []

htmlhelp_basename = 'TCFPythondoc'

intersphinx_mapping = {'http://docs.python.org/': None}
