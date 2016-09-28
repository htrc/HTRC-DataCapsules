from setuptools import setup

setup(name='negotiator-cli',
      version='0.1',
      description='Tool for interacting QEMU VMs with negotiator-guest running.',
      url='http://github.com/milinda/negotiator-cli',
      author='Milinda Pathirage',
      author_email='milinda.pathirage@gmail.com',
      license='Apache License, Version 2',
      install_requires=[
          'negotiator-host',
          'coloredlogs'
      ])
