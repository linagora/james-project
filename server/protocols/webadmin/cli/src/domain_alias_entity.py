import click
import json
import requests as req
import sys


def print_json(obj):
    parsed = json.loads(obj.text)
    print(json.dumps(parsed, indent=4, sort_keys=True))


def handle_other_responses(res):
    print("Unhandled response received from the server.")
    print("Status: " + str(res.status_code))
    print_json(res)
    sys.exit()


@click.group()
def domain_alias():
    """Domain's alias implementation"""
    pass


@domain_alias.command(name="list")
@click.pass_context
@click.argument("domain_name", type=str)
def get_domain_aliases(ctx, domain_name):
    """Show all aliases of a domain"""
    res = req.get(ctx.obj["path"] + "/domains/" + domain_name + "/aliases")
    if res.status_code == 200:
        print_json(res)
    elif res.status_code == 400:
        print("Destination has an invalid syntax.")
        sys.exit()
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 404:
        print("The domain does not exist.")
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)


@domain_alias.command(name="add")
@click.pass_context
@click.argument("source_domain", type=str)
@click.argument("destination_domain", type=str)
def add_alias(ctx, source_domain, destination_domain):
    """Add an alias to a destination domain"""

    res = req.get(ctx.obj["path"] + "/domains/" + source_domain)
    if res.status_code != 204:
        print("Cannot create alias %s becase it does not exist. Please create it first" % source_domain)
        return

    res = req.put(ctx.obj["path"] + "/domains/" +
                  destination_domain + "/aliases/" + source_domain)
    if res.status_code == 204:
        print("%s has been added to %s" % (source_domain, destination_domain))
    elif res.status_code == 400:
        if (source_domain == destination_domain):
            print("Source domain and destination domain are the same")
        else:
            print("Source domain or destination domain has an invalid syntax.")
        sys.exit()
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 404:
        print("%s does not exist." % source_domain)
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)


@domain_alias.command(name="remove")
@click.pass_context
@click.argument("source_domain", type=str)
@click.argument("destination_domain", type=str)
def remove_alias(ctx, source_domain, destination_domain):
    """Remove an alias from a destination domain"""
    res = req.delete(ctx.obj["path"] + "/domains/" +
                     destination_domain + "/aliases/" + source_domain)
    if res.status_code == 204:
        print("%s has been removed from %s" %
              (source_domain, destination_domain))
    elif res.status_code == 400:
        if (source_domain == destination_domain):
            print("Source domain and destination domain are the same")
        else:
            print("Source domain or destination domain has an invalid syntax.")
        sys.exit()
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 404:
        print("%s does not exist." % source_domain)
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)
