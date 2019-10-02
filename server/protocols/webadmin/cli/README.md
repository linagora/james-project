# Apache James Server/Webadmin command line interface

## Development

'Webadmin command-line interface is an upcoming replacement for the outdated, security-vulnerable JMX command-line interface. It also aims at providing a more modern and intuitive interface.
It is written in Python using Click - a package that supports creating command-line interfaces. Further information about Click can be found [here](https://click.palletsprojects.com).


## Run the script
1. Enter the following command to get Click installed

      ```
      $ pip install Click
      ```
  
2. Navigate to the location of 'james_cli'

      Example: 
      ```
      $ cd Desktop/james-project/james_cli 
      ```
  
3. Try out 
      ```
      $ ./james_cli' to show the helping screen 
      ```


## Syntax

General syntax to run the script

        
        $ ./james_cli [OPTION] ENTITY ACTION {ARGUMENT}
        
where

    OPTION: optional parameter when running the script,
  
    ENTITY: represents the route performing actions,
  
    ACTION: name of the action to perform,
  
    ARGUMENT: arguments needed for the action.

Example: 
```
$ ./james_cli --url http://127.0.0.1 --port 9999 domain list
```

The above command lists all domain names available on domain route at address http://127.0.0.1:9999. 
It does not require any argument to execute. Options --url and --port are optional. Without them, the default value is http://127.0.0.0:8000.
As for other commands, arguments might be required after the sub-command (ACTION such as list, add and remove).

## Available Entities
  - [Domain](#Domain)
  - [Domain alias](#Domain_alias)
  - [Mapping](#Mapping)
  
## Domain 
Available actions on domain entity
  - [Add domain](#Add_domain)
  - [Check domain existence](#Check_domain_exist)
  - [List all domains](#List_all_domains)
  - [Remove domain](#Remove_domain)
  
### Add domain
Add a domain to the domain list.
```
./james_cli domain add <domainToBeAdded>
```

### Check domain existance
Check whether a domain exists on the domain list or not.
```
./james_cli domain exist <domainToBeChecked>
```

### List all domains
Show all domains' name on the list.
```
./james_cli domain list
```

### Remove domain
Remove a domain from the domain list.
```
./james_cli domain remove <domainToBeRemoved>
```

## Domain alias
Available actions on domain alias entity
  - [Add domain alias](#Add_domain_alias)
  - [List domain aliases](#List_domain_aliases)
  - [Remove domain alias](#Remove_domain_alias)
  
### Add domain alias
Add an alias to a destination domain
```
./james_cli domain-alias <aliasToBeAdded> <destinationDomain>
```

### List domain aliases
List all aliases of a destionation domain
```
./james_cli domain-alias <destinationDomain>
```

### Remove domain alias
Remove an alias from a destination domain
```
./james_cli domain-alias remove <aliasToBeRemoved> <destinationDomain>
```

## Mapping
Available action on mapping entity
  - [List mappings](#List_mappings)

### List mappings
List all available mappings
```
./james_cli mapping list
```

Whenever being confused with the syntax, you can also use option: '--help' to get detail information on how to use a command.
Example: Type ```$ ./james_cli domain --help``` to see its instruction and sub-commands.