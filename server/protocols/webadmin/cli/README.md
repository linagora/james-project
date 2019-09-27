# Apache James Server/Webadmin command line interface

## Development

'Webadmin command-line interface is an upcoming replacement for the outdated, security-vulnerable JMX command-line interface. It also aims at providing a more modern and intuitive interface.
It is written in Python using Click - a package that supports creating command-line interfaces. Further information about Click can be found [here](https://click.palletsprojects.com).


## Run the script
1. Enter the following command to get Click installed

      ```$ pip install Click```
  
2. Navigate to the location of 'james_cli'.

      Example: ```$ cd Desktop/james-project/james_cli ```
  
3. Type ```$ ./james_cli' to show the helping screen ```


## Syntax

General syntax to run the script

        ```$ ./james_cli [OPTION] [ENTITY] [ACTION] [ARGUMENT]...```
where

    [OPTION] optional parameter when running the script,
  
    [ENTITY] represents the route that will be implemented,
  
    [ACTION] name of the action to perform,
  
    [ARGUMENT] argument needed for the action.

Example: ```$ ./james_cli --ip 127.0.0.1 --port 8000 domain list```

The above command will list all domain name available on domain route at address http://127.0.0.1:8000

In fact, the default ip and port value are 127.0.0.1 and 8000 respectively.

Whenever being confused with the syntax, please use option: '--help' to get detail information on how to use a command.
Example: Type ```$ ./james_cli domain --help``` to see its instruction and sub-commands.