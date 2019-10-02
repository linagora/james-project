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
def domain():
    """Domain routes implementation"""
    pass


@domain.command(name="list")
@click.pass_context
def get_all_domains(ctx):
    """Show all available domains"""
    res = req.get(ctx.obj["path"] + "/domains")

    if res.status_code == 200:
        print("List of all available domains: ")
        print_json(res)
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)


@domain.command(name="add")
@click.pass_context
@click.argument("domain_name", type=str)
def add_domain(ctx, domain_name):
    """Add a domain to the domain list"""

    res = req.put(ctx.obj["path"] + "/domains/" + domain_name)

    if res.status_code == 204:
        print("%s has been added." % domain_name)
    elif res.status_code == 400:
        print("The domain name is invalid.")
        sys.exit()
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)


@domain.command(name="remove")
@click.pass_context
@click.argument("domain_name", type=str)
def delete_domain(ctx, domain_name):
    """Remove a domain from the domain list"""
    res = req.delete(ctx.obj["path"] + "/domains/" + domain_name)

    if res.status_code == 204:
        print("%s has been removed." % domain_name)
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)


@domain.command(name="exist")
@click.pass_context
@click.argument("domain_name", type=str)
def check_existence(ctx, domain_name):
    """Check whether a domain is existing or not"""
    res = req.get(ctx.obj["path"] + "/domains/" + domain_name)
    if res.status_code == 204:
        print("%s does exist." % domain_name)
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 404:
        print("%s does NOT exist." % domain_name)
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)
