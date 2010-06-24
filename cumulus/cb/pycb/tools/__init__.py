import pynimbusauthz 

def report_options_to_string(opts, delim=","):
    rc = ""
    d = ""
    for o in opts:
        rc = rc + d + o
        d = delim 
    return rc

def print_report(report_obj, cols, opts):
    choices = cols.split(",")

    out_line = ""
    d = opts.delim
    delim = ""
    for c in choices:
        v = getattr(report_obj, c)
        v = str(v)
        if opts.batch:
            out_line = out_line + delim + v
            delim = d
        else:
            c = c.replace('_', " ")
            pynimbusauthz.print_msg(opts, 0,  "%-15s : %s" % (c, v))

    pynimbusauthz.print_msg(opts, 0,  out_line)
