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
def mapping():
    """Mapping routes implementation"""
    pass


@mapping.command(name="list")
@click.pass_context
def get_all_mapping(ctx):
    """List all mappings"""
    res = req.get(ctx.obj["path"] + "/mappings")
    if res.status_code == 200:
        print_json(res)
    elif res.status_code == 401:
        print("Authentication error.")
        sys.exit()
    elif res.status_code == 500:
        print("Internal server error!")
        sys.exit()
    else:
        handle_other_responses(res)
