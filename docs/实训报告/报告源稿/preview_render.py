# -*- coding: utf-8 -*-
"""粗略渲染 draw.io 图以便目视检查布局（非最终成品）。"""
import sys, xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont


def render(path, out, scale=0.5):
    root = ET.parse(path).getroot()
    model = root.find('.//mxGraphModel')
    W = int(model.get('pageWidth')) + 60
    H = int(model.get('pageHeight')) + 60
    img = Image.new('RGB', (int(W * scale), int(H * scale)), 'white')
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc', max(9, int(14 * scale)))
        fontb = ImageFont.truetype(r'C:\Windows\Fonts\msyhbd.ttc', max(9, int(17 * scale)))
    except Exception:
        font = fontb = ImageFont.load_default()

    def S(v):
        return v * scale

    cells = {}
    order = []
    for c in root.iter('mxCell'):
        cid = c.get('id')
        cells[cid] = c
        order.append(cid)

    # 先画连线
    for cid in order:
        c = cells[cid]
        if c.get('edge') != '1':
            continue
        geo = next((ch for ch in c if ch.tag.endswith('mxGeometry')), None)
        if geo is None:
            continue
        sp = geo.find('.//mxPoint[@as="sourcePoint"]')
        tp = geo.find('.//mxPoint[@as="targetPoint"]')
        src = c.get('source'); tgt = c.get('target')
        style = c.get('style', '')

        def anchor(cid_):
            cc = cells.get(cid_)
            if cc is None or cc.get('vertex') != '1':
                return None
            g = next((ch for ch in cc if ch.tag.endswith('mxGeometry')), None)
            if g is None:
                return None
            return (float(g.get('x')) + float(g.get('width')) / 2,
                    float(g.get('y')) + float(g.get('height')) / 2)

        a = anchor(src) if src else None
        b = anchor(tgt) if tgt else None
        if sp is not None:
            a = (float(sp.get('x')), float(sp.get('y')))
        if tp is not None:
            b = (float(tp.get('x')), float(tp.get('y')))
        if a is None or b is None:
            continue
        pts = [a]
        arr = geo.find('.//Array[@as="points"]')
        if arr is not None:
            pts += [(float(p.get('x')), float(p.get('y'))) for p in arr]
        pts.append(b)
        dash = 'dashed' in style
        color = '#888888' if 'strokeColor=#666666' in style else '#333333'
        for i in range(len(pts) - 1):
            x1, y1 = pts[i]; x2, y2 = pts[i + 1]
            if dash:
                # 简单虚线
                import math
                L = math.hypot(x2 - x1, y2 - y1)
                n = max(1, int(L / 8))
                for k in range(n):
                    if k % 2:
                        continue
                    t1, t2 = k / n, (k + 0.6) / n
                    d.line([S(x1 + (x2 - x1) * t1), S(y1 + (y2 - y1) * t1),
                            S(x1 + (x2 - x1) * t2), S(y1 + (y2 - y1) * t2)], fill=color, width=1)
            else:
                d.line([S(x1), S(y1), S(x2), S(y2)], fill=color, width=1)

    # 再画形状
    for cid in order:
        c = cells[cid]
        if c.get('vertex') != '1':
            continue
        g = next((ch for ch in c if ch.tag.endswith('mxGeometry')), None)
        if g is None:
            continue
        x, y = float(g.get('x')), float(g.get('y'))
        w, h = float(g.get('width')), float(g.get('height'))
        style = c.get('style', '')
        label = c.get('value', '').replace('&lt;br&gt;', '\n')
        if 'text;' in style:
            d.multiline_text((S(x), S(y)), label, fill='#555555', font=font)
            continue
        if 'rhombus' in style:
            d.polygon([S(x + w / 2), S(y), S(x + w), S(y + h / 2), S(x + w / 2), S(y + h), S(x), S(y + h / 2)],
                      outline='#bf8f00', fill='#ffe6cc')
            f = font
        elif 'ellipse' in style:
            d.ellipse([S(x), S(y), S(x + w), S(y + h)], outline='#2f5597', fill='#ffffff')
            f = font
        elif 'dashed=1' in style:
            d.rectangle([S(x), S(y), S(x + w), S(y + h)], outline='#999999', fill='#f5f5f5')
            f = fontb
        else:
            d.rectangle([S(x), S(y), S(x + w), S(y + h)], outline='#2f5597', fill='#dae8fc')
            f = fontb
        if label:
            tw = d.textlength(label, font=f)
            d.text((S(x + w / 2) - tw / 2, S(y + h / 2) - 7), label, fill='#111111', font=f)
    img.save(out)
    print('渲染:', out, img.size)


if __name__ == '__main__':
    render(sys.argv[1], sys.argv[2], float(sys.argv[3]) if len(sys.argv) > 3 else 0.5)
