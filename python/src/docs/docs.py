# *****************************************************************************
# * Copyright (c) 2016 Wind River Systems, Inc. and others.
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the Eclipse Public License 2.0
# * which accompanies this distribution, and is available at
# * https://www.eclipse.org/legal/epl-2.0/
# *
# * Contributors:
# *     Wind River Systems - initial implementation
# *****************************************************************************

"""TCF Python documentation tool.

This tool is based on `Sphinx <http://www.sphinx-doc.org>`_, and generates
documentation for the reStructuredText files located in the ``rst`` directory.

Usage
=====
``python docs.py [-h] [-outdir OUTDIR] [-sphinxdir SPHINXDIR] [-theme THEME] \
{generate,clean,cleanall}``

Command line options
====================

  +----------------+----------------------------------------------------+
  | ``-h``,        | Show this help message and exit.                   |
  | ``--help``     |                                                    |
  +----------------+----------------------------------------------------+
  | ``-outdir``    | Directory to generate documentation into.          |
  +----------------+----------------------------------------------------+
  | ``-sphinxdir`` | Directory to install sphinx into *(not for Windows |
  |                | hosts)*.                                           |
  +----------------+----------------------------------------------------+
  | ``-theme``     | The sphinx theme used for generated documentation. |
  +----------------+----------------------------------------------------+

Command line arguments
======================

``command``
-----------
The ``command`` value may take any of the following values:

- ``generate`` : Generates the html documentation using Sphinx.
- ``clean`` : Removes the generated documentation from output directory.
- ``cleanall`` : Removes the generated documentation from output directory, and
  the Sphinx installation.
"""

import argparse
import glob
import os
import shutil
import subprocess
import sys
import tarfile
import time
import zipfile

try:
    # Python 3
    import urllib.request as URLLIB  # @UnresolvedImport  @UnusedImport
except:
    import urllib as URLLIB  # @UnresolvedImport  @UnusedImport @Reimport


# A bit of python 2/3 compat. The ../tcf/compat.py cannot be used as this
# is a different package. Full path should be used.

ispython3 = sys.version_info[0] == 3

if ispython3:
    strings = (str,)
else:
    strings = (basestring, str)  # @UndefinedVariable


def bytes2str(data):
    """Converts bytes to a string.

    The integers of *data* are returned into a single string. This is
    mainly used to transform a |bytearray| into a string the same way for
    python 2 or 3.

    :param data: An iterable of integers (value smaller than 256 - aka
                 unsigned char values).

    :returns: An |str| representing *data* converted to characters.
    """
    if data and isinstance(data[0], int):
        return ''.join(chr(b) for b in data)
    elif data:
        return str(data)
    return ''


def execute(command, path=None, msg=None, verbose=False):
    """Execute given command in given path.

    :param command: The list of arguments of the command to execute.
    :type command: |list|
    :param path: The path to execute command at. If **None**, current path is
                 used.
    :type path: |str| or **None**
    :param msg: The message to print out for the command execution. If
                **None**, the actual command is printed out.
    :type msg: |str| or **None**
    :param verbose: States if command output should be printed out.
    :type verbose: |bool|
    """
    if path is None:
        path = os.getcwd()
    if msg is None:
        msg = ' '.join(command)

    msg += ' ... '

    sys.stdout.write(msg)
    sys.stdout.flush()
    if verbose:
        sys.stdout.write('\n')
        prc = subprocess.Popen(command, cwd=path)
    else:
        prc = subprocess.Popen(command, cwd=path, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT)
    _out, err = prc.communicate()
    if err:
        sys.stderr.write('fail\n')
        sys.stderr.write(str(err) + '\n')
        raise Exception(err)
    else:
        sys.stdout.write('done\n')


def fullpath(path):
    """Get the fullpath of given *path*.

    If path is a tuple or a list, elements of *path* are first joined with OS
    separator to create the *path* string.

    If *path* is a string, it is user expanded and variables expanded.

    :param path: The path to get full path for.
    :type path: |str| or |tuple| or |list|

    :returns: An |str| representing the full path of *path*.
    """
    if path is None:
        return path

    if isinstance(path, strings):
        pathstr = path
    elif isinstance(path, (list, tuple)):
        pathstr = os.path.join(*path)
    else:
        raise Exception('Unsupported file path "' + str(path) +
                        '": invalid type ' + str(type(path)))
    res = os.path.expandvars(pathstr)
    res = os.path.expanduser(res)
    if not os.path.isabs(res):
        res = os.path.abspath(res)
    res = os.path.realpath(res)
    res = os.path.normpath(res)

    return res


def wget(url, destdir):
    """Download a file from an URL to a directory.

    If *directroy* does not exist, it is created.

    :param url: The URL to download file from.
    :type url: |str|
    :param destdir: The directory to download file to.
    :type destdir: |str|
    """
    if not os.path.exists(destdir):
        os.makedirs(destdir)

    destfile = fullpath((destdir, os.path.basename(url)))

    # Remove the destination file if needed

    if os.path.exists(destfile):
        os.remove(destfile)

    # Determine the destination file name

    ix = 1
    destbase = destfile

    while os.path.exists(destfile):
        destfile = destbase + '.' + str(ix)
        ix += 1

    URLLIB.urlretrieve(url, os.path.join(destdir, destfile))

    filesize = os.path.getsize(destfile)

    msg = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime()) + ' URL:' + \
        url + ' [' + str(filesize) + '/' + str(filesize) + '] -> "' + \
        destfile + '" [1]'

    sys.stdout.write(msg + '\n')

    return destfile


class Sphinx(object):

    BUILDER_HTML = 'html'

    THEME_AGOGO = 'agogo'
    THEME_BASIC = 'basic'
    THEME_BIZSTYLE = 'bizstyle'
    THEME_CLASSIC = 'classic'
    THEME_DEFAULT = 'default'
    THEME_EPUB = 'epub'
    THEME_HAIKU = 'haiku'
    THEME_NATURE = 'nature'
    THEME_PYRAMID = 'pyramid'
    THEME_SCROLLS = 'scrolls'
    THEME_SPHINXDOC = 'sphinxdoc'
    THEME_TRADITIONAL = 'traditional'

    THEMES = (THEME_AGOGO, THEME_BASIC, THEME_BIZSTYLE, THEME_CLASSIC,
              THEME_DEFAULT, THEME_EPUB, THEME_HAIKU, THEME_NATURE,
              THEME_PYRAMID, THEME_SCROLLS, THEME_SPHINXDOC, THEME_TRADITIONAL)

    VERSION = '1.4.1'

    """An object representing a Sphinx install.

    :param path: The path to install Sphinx packages in. If **None**, the
                 current python interpreter directory is used.
    :type path: |str| or **None**
    """
    def __init__(self, path=None):

        if path:
            self._path = path
        else:
            self._path = fullpath(((sys.executable), '..', '..'))

        self._bin = None
        self._builder = self.BUILDER_HTML
        self._cachedir = None
        self._lib = None
        self._name = 'Sphinx-' + self.VERSION
        self._outdir = None
        self._rstdir = fullpath(('.', 'rst'))
        if ispython3:
            self._theme = self.THEME_PYRAMID
        else:
            self._theme = self.THEME_DEFAULT
        self._url_base = 'https://pypi.python.org/packages/source/S/Sphinx'
        self._url = self._url_base + '/' + self._name + '.tar.gz'
        self._egg = self._name + '-py' + str(sys.version_info[0]) + '.' + \
            str(sys.version_info[1]) + '.egg'
        self._dependency = self.lib + '/site-packages/' + self._egg
        self._version = self.VERSION

    @property
    def bin(self):
        """The binary directory for the current python interpreter."""
        if self._bin is None:
            if sys.platform in ('win32', 'cygwin'):
                self._bin = fullpath((sys.executable, '..'))
            else:
                self._bin = fullpath((self.path, 'bin'))
        return self._bin

    def build(self, builder=None):
        """Build documentation with given builder."""
        if builder:
            self._builder = builder
        if sys.platform in ('win32', 'cygwin'):
            command = [self.script]
        else:
            command = [sys.executable, '-u', '-B', self.script]
        command += self.options + [self._rstdir, self.outdir]
        execute(command, self.outdir, None, True)

    @property
    def builder(self):
        """The builder name.

        .. seealso:: http://www.sphinx-doc.org/en/stable/builders.html#builders
        """
        return self._builder

    @property
    def cachedir(self):
        """The directory where cache files are generated."""
        if self._cachedir is None:
            self._cachedir = fullpath((self.outdir, 'cache'))
        return self._cachedir

    def install(self):
        """Install Sphinx if needed."""
        # On Windows, install with pip, and assume setuptools is there.
        if not self.script or not os.path.exists(self.script):
            if sys.platform in ('win32', 'cygwin'):
                self._install_windows()
            else:
                self._install_unix()

    @property
    def installed(self):
        return self.script and os.path.exists(self.script)

    @property
    def lib(self):
        """Sphinx python library path."""
        if self._lib is None:
            pyversion = 'python' + str(sys.version_info[0]) + '.' + \
                        str(sys.version_info[1])
            if sys.platform in ('win32', 'cygwin'):
                self._lib = fullpath((sys.executable, '..', '..', 'Lib'))
            else:
                self._lib = fullpath((self.path, 'lib', pyversion))
        return self._lib

    @property
    def options(self):
        """The list of options used at doc generation time.

        :returns: A |list| of |str| representing the list of options used
                  by this Sphinx installation as doc generation time.
        """
        _options = ['-d', self.cachedir, '-b', self.builder]

        if self.builder is self.BUILDER_HTML:
            _options += ['-D', 'html_theme=' + self.theme]

        return _options

    @property
    def outdir(self):
        """Output directory for this documentation parser."""
        if self._outdir is None:
            self._outdir = fullpath((self.path, self.builder))
        return self._outdir

    @outdir.setter
    def outdir(self, value):
        """Set the output directory for this documentation parser."""
        if value:
            # create directory if possible.
            value = fullpath(value)
            if not os.path.exists(value):
                try:
                    os.makedirs(value)
                except Exception as e:
                    msg = 'Could not create directory "' + str(value) + \
                          '". Got error : "' + str(e) + '".'
                    raise Exception(msg)
            self._outdir = value
        else:
            self._outdir = None

    @property
    def path(self):
        """The path this Sphinx utility is installed in."""
        return self._path

    @property
    def pythonpath(self):
        """The python path to use sphinx.

        :returns: A |list| of python paths to use with this Sphinx
                  installation.
        """
        if not hasattr(self, '_pythonpath') or self._pythonpath is None:
            sitepkgs = fullpath((self.lib, 'site-packages'))
            if sys.platform in ('win32', 'cygwin'):
                # Use the python install on Windows.
                self._pythonpath = []
            else:
                self._pythonpath = [self.lib, sitepkgs]
            try:
                if not os.path.exists(self.lib):
                    os.makedirs(self.lib)
                if not os.path.exists(sitepkgs):
                    os.makedirs(sitepkgs)
            except OSError as ose:
                # Check if it is a 'Permission denied' error.
                if ose.errno == 13:
                    msg = 'Could not install Sphinx, you need to have '\
                          'administrator permissions, or use the -sphinxdir '\
                          'option.'
                    raise Exception(msg)
                else:
                    raise ose
            except Exception as e:
                raise e
        return self._pythonpath

    @property
    def script(self):
        """Path of this sphinx installation build script.

        :returns: An |str| representing the path to this Sphinx installation
                  build script, or **None** if the script could not be found.
        """
        if not hasattr(self, '_script') or self._script is None or \
           not os.path.exists(self._script):
            self._script = None
            # Try to find it ...
            if sys.platform in ('win32', 'cygwin'):
                path = fullpath((self.bin, 'Scripts', 'sphinx-build.exe'))
            else:
                path = fullpath((self.bin, 'sphinx-build'))

            if os.path.exists(path):
                self._script = path

        return self._script

    @property
    def theme(self):
        """The current theme for generated documentation."""
        return self._theme

    @theme.setter
    def theme(self, value):
        """Set the current theme for generated documentation."""
        if value in self.THEMES:
            self._theme = value
        elif value is None:
            if ispython3:
                self._theme = Sphinx.THEME_PYRAMID
            else:
                self._theme = self.THEME_DEFAULT
        else:
            raise Exception('Unknown Sphinx theme "' + value +
                            '". Please use one of: ' + ' ' .join(self.themes) +
                            '.')

    @property
    def themes(self):
        """The list of possible themes for documentation.

        :returns: A |tuple| of |str| listing the possible themes for this
                  Sphinx installation.
        """
        return self.THEMES

    # -------------------------- protected methods -------------------------- #

    def _install_unix(self):
        if not self.script or not os.path.exists(self.script):
            # Go and install it. This may require a sudo from the user on
            # Linux hosts.

            msg = 'Installing ' + self._name + ' in ' + self.path + '.\n'
            msg += 'Getting ' + self._name + ' from "' + self._url_base + '".'
            sys.stdout.write(msg + '\n')

            # Unset the PYTHONDONTWRITEBYTECODE variable if set, or some
            # 'setup.py build' fail.

            savedenv = {}
            if 'PYTHONDONTWRITEBYTECODE' in os.environ:
                name = 'PYTHONDONTWRITEBYTECODE'
                savedenv[name] = os.environ.pop(name)

            dldir = os.path.join(self.path, 'download')
            instargs = (['build'], ['install', '--prefix', self._path])

            # Check if setuptools is installed.

            try:
                import setuptools  # @UnresolvedImport @UnusedImport
            except ImportError:
                # Check if setuptools is in the site-packages.
                sitepkgs = fullpath((self.lib, 'site-packages'))
                if not glob.glob(os.path.join(sitepkgs, 'setuptools*')):
                    # Install it too.
                    sturl = 'https://pypi.io/packages/source/s/setuptools/'\
                            'setuptools-24.0.2.zip'
                    filepath = wget(sturl, dldir)
                    zf = zipfile.ZipFile(filepath, 'r')
                    zf.extractall(dldir)
                    zf.close()
                    # Run the build and install steps
                    stdir = fullpath((dldir, 'setuptools-24.0.2'))

                    for arguments in instargs:
                        prcargs = [sys.executable,
                                   fullpath((stdir, 'setup.py'))]
                        prcargs += arguments
                        msg = 'setuptools-24.0.2 ' + ' '.join(arguments)
                        execute(prcargs, stdir, msg)

            # Now that the setuptools package is here, we may install sphinx
            # too.

            filepath = wget(self._url, dldir)
            tar = tarfile.open(filepath)
            os.chdir(dldir)
            tar.extractall()
            tar.close()

            # Run the build and install steps

            for arguments in instargs:
                spdir = fullpath((dldir, self._name))
                prcargs = [sys.executable, fullpath((spdir, 'setup.py'))]
                prcargs += arguments
                msg = self._name + ' ' + ' '.join(arguments)
                execute(prcargs, spdir, msg, True)

            # Restore modified env variables if needed

            for varname, value in savedenv.items():
                os.environ[varname] = value

    def _install_windows(self):
        # With Windows hosts, assume setuptools is installed, and use pip to
        # get Sphinx.

        python = fullpath(sys.executable)
        pyinstall = os.path.dirname(python)
        pip = fullpath((pyinstall, 'Scripts', 'pip.exe'))

        # Update pip if needed

        command = [python, '-m', 'pip', 'install', '--upgrade', 'pip']
        execute(command, pyinstall)

        # Install sphinx

        command = [pip, 'install', 'sphinx']
        execute(command, pyinstall)


class HtmlDocumentation(argparse.ArgumentParser):

    COMMAND_GENERATE = 'generate'
    COMMAND_CLEAN = 'clean'
    COMMAND_CLEANALL = 'cleanall'
    COMMAND_DEFAULT = COMMAND_GENERATE
    COMMANDS = (COMMAND_CLEAN, COMMAND_CLEANALL, COMMAND_GENERATE)

    """An Html documentation tool based on Sphinx."""
    def __init__(self):
        self._command = None
        self._sphinx = None

        # In order to install sphinx in a folder we are sure to be allowed to
        # install, use this git repo in the: org.eclipse.tcf.python/docs
        # folder.

        # Current __file__ is org.eclipse.tcf.python/src/docs/docs.py
        self._outdir = fullpath((__file__, '..', '..', '..', 'docs', 'html'))
        self._sphinxdir = fullpath((self._outdir, '..', 'sphinx'))

        super(HtmlDocumentation, self).__init__('Tool for TCF python HTML '
                                                'documentation.')

        # Add command line options.

        self.add_argument('-outdir',
                          help='Directory to generate documentation into. '
                               'Default is "' + self._outdir + '".')
        if sys.platform not in ('win32', 'cygwin'):
            self.add_argument('-sphinxdir',
                              help='Directory to install sphinx into. '
                                   'Default is "' + self._sphinxdir + '".')
        self.add_argument('-theme', choices=self.sphinx.themes,
                          help='The sphinx theme used for generated '
                               'documentation.')

        self.add_argument('command', choices=self.COMMANDS,
                          help='The name of the command to execute.')

    @property
    def command(self):
        return self._command

    @command.setter
    def command(self, value):
        """Set the name of the current command."""
        if value in self.COMMANDS:
            self._command = value
        else:
            self._command = self.COMMAND_DEFAULT

    def clean(self):
        """Clean the output directory content.

        .. seealso:: |outdir|
        """
        sys.stdout.write('Removing directory "' + str(self.outdir) + '" ... ')
        if self.outdir and os.path.exists(self.outdir):
            shutil.rmtree(self.outdir)
        sys.stdout.write('done\n')

    def cleanall(self):
        """Clean the output directory content and the sphinx directory content.

        .. seealso:: |outdir|, |sphinxdir|
        """
        self.clean()
        if sys.platform in ('win32', 'cygwin'):
            # With Windows hosts, assume setuptools is installed, and use pip
            # to remove Sphinx.

            python = fullpath(sys.executable)
            pyinstall = os.path.dirname(python)
            pip = fullpath((pyinstall, 'Scripts', 'pip.exe'))

            # Update pip if needed

            command = [python, '-m', 'pip', 'install', '--upgrade', 'pip']
            execute(command, pyinstall)

            # Install sphinx

            command = [pip, 'uninstall', '-y', 'sphinx']
            execute(command, pyinstall, None, True)
        else:
            sys.stdout.write('Removing directory "' + str(self.sphinxdir) +
                             '" ... ')
            if self.sphinxdir and os.path.exists(self.sphinxdir):
                shutil.rmtree(self.sphinxdir)
            sys.stdout.write('done\n')

    def execute(self):
        """Execute the current command."""
        getattr(self, self.command)()

    def generate(self):
        """Command to generate html documentation."""
        # Override PYTHONPATH : insert TCF project path and sphinx install
        # paths.
        ppath = self.sphinx.pythonpath + sys.path
        ppath.insert(0, fullpath('..'))

        os.environ['PYTHONPATH'] = os.pathsep.join(ppath)

        self.sphinx.install()
        self.sphinx.outdir = self.outdir
        self.sphinx.theme = self.theme
        self.sphinx.build('html')

    @property
    def outdir(self):
        """Output directory for this documentation parser."""
        return self._outdir

    @outdir.setter
    def outdir(self, value):
        """Set the output directory for this documentation parser."""
        if value:
            # create directory if possible.
            value = fullpath(value)
            if not os.path.exists(value):
                try:
                    os.makedirs(value)
                except Exception as e:
                    msg = 'Could not create directory "' + str(value) + \
                          '". Got error : "' + str(e) + '".'
                    raise Exception(msg)
            self._outdir = value
        else:
            self._outdir = None

    @property
    def sphinx(self):
        """The current sphinx installation definition."""
        if not self._sphinx:
            self._sphinx = Sphinx(self.sphinxdir)
        return self._sphinx

    @property
    def sphinxdir(self):
        """Sphinx installation directory."""
        return self._sphinxdir

    @sphinxdir.setter
    def sphinxdir(self, value):
        """Set the sphinx installation directory."""
        if value:
            # create directory if possible.
            value = fullpath(value)
            if not os.path.exists(value):
                try:
                    os.makedirs(value)
                except Exception as e:
                    msg = 'Could not create directory "' + str(value) + \
                          '". Got error : "' + str(e) + '".'
                    raise Exception(msg)
            self._sphinxdir = value
            self._sphinx = None
        else:
            self._sphinxdir = None

    @property
    def theme(self):
        """The current theme for the generated documentation."""
        if not hasattr(self, '_theme') or self._theme is None:
            if ispython3:
                self._theme = Sphinx.THEME_PYRAMID
            else:
                self._theme = Sphinx.THEME_DEFAULT
        return self._theme

    @theme.setter
    def theme(self, value):
        """Set the current theme for generated documentation."""
        if value and value in Sphinx.THEMES:
            self._theme = value
        else:
            self._theme = None


if __name__ == "__main__":
    try:
        doc = HtmlDocumentation()
        doc.parse_args(namespace=doc)
        doc.execute()
    except Exception as e:
        sys.stderr.write('Got exception "' + str(e) + '".')
