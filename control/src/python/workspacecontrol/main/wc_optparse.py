# This is set up in such a way that you should not need to alter this file to
# add an argument, in most cases.

import optparse
import wc_args

WC_VERSION = "2.7"

# -----------------------------------------------------------------------------

def _add_option(group, arg):
    if arg.boolean:
        _add_boolean_option(group, arg)
    elif arg.string:
        _add_string_option(group, arg)
    else:
        raise Exception("unknown arg type")
        
def _add_string_option(group, arg):
    if arg.short_syntax:
        group.add_option(arg.short_syntax, arg.long_syntax,
                         dest=arg.dest, help=arg.help,
                         metavar=arg.metavar)
    else:
        group.add_option(arg.long_syntax,
                         dest=arg.dest, help=arg.help,
                         metavar=arg.metavar)

def _add_boolean_option(group, arg):
    if arg.short_syntax:
        group.add_option(arg.short_syntax, arg.long_syntax,
                         dest=arg.dest, help=arg.help,
                         action="store_true", default=False)
    else:
        group.add_option(arg.long_syntax,
                         dest=arg.dest, help=arg.help,
                         action="store_true", default=False)

# -----------------------------------------------------------------------------

def parsersetup():
    """Return configured command-line parser."""

    ver="Workspace Control %s - http://www.nimbusproject.org" % WC_VERSION
    usage="see help (-h)."
    parser = optparse.OptionParser(version=ver, usage=usage)

    # ----
    
    # Might be helpful to have more groups in the future.
    deprecated_args = []
    create_args = []
    other_args = []
    
    for arg in wc_args.ALL_WC_ARGS_LIST:
        if arg.deprecated:
            deprecated_args.append(arg)
        elif arg.createarg:
            create_args.append(arg)
        else:
            other_args.append(arg)
            
            
    # ---------------------------------------
            
    grouptxt1 = "  Arguments"
    grouptxt2 = "  Creation arguments"
    grouptxt3 = "  Deprecated"
    # For each one, use twice length of the longest one:
    groupline = (len(2*grouptxt2)-1) * "-"
    
    # 1 - other_args
    group = optparse.OptionGroup(parser, grouptxt1, groupline)
    for arg in other_args:
        _add_option(group, arg)
    parser.add_option_group(group)
    
    # 2 - create_args
    group = optparse.OptionGroup(parser, grouptxt2, groupline)
    for arg in create_args:
        _add_option(group, arg)
    parser.add_option_group(group)
    
    # 3 - deprecated_args
    group = optparse.OptionGroup(parser, grouptxt3, groupline)
    for arg in deprecated_args:
        _add_option(group, arg)
    parser.add_option_group(group)
    
    return parser
    
