#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./integration_tests.sh URL BRANCH JAMES_IP JAMES_IMAP_PORT"
   echo "    JAMES_IP: IP of the James server to be tests"
   echo "    JAMES_IMAP_PORT: Exposed IMAP port of this James server"
   echo "    URL : URL of the repository to pull"
   echo "    SHA1(optional): Branch to build or trunk if none"
   exit 1
}

ORIGIN=/origin

for arg in "$@"
do
   case $arg in
      -*)
         echo "Invalid option: -$OPTARG"
         printUsage
         ;;
      *)
         if ! [ -z "$1" ]; then
            JAMES_ADDRESS=$1
         fi
         if ! [ -z "$2" ]; then
            JAMES_IMAP_PORT=$2
         fi
         if ! [ -z "$3" ]; then
            URL=$3
         fi
         if ! [ -z "$4" ]; then
            SHA1=$4
         fi
         ;;
   esac
done

if [ -z "$JAMES_ADDRESS" ]; then
   echo "You must provide a JAMES_ADDRESS"
   printUsage
fi

if [ -z "$JAMES_IMAP_PORT" ]; then
   echo "You must provide a JAMES_IMAP_PORT"
   printUsage
fi

if [ -z "$URL" ]; then
   echo "You must provide a URL"
   printUsage
fi

if [ -z "$SHA1" ]; then
   SHA1=trunk
fi

export JAMES_ADDRESS=$JAMES_ADDRESS
export JAMES_IMAP_PORT=$JAMES_IMAP_PORT

git clone $URL
cd james-project
git checkout $SHA1

mvn -Dtest=ExternalJamesTest -DfailIfNoTests=false -pl org.apache.james:apache-james-mpt-external-james -am test
