"""docs/proposal.md를 공모전 접수양식(DOCX) 02 파트 칸에 채워 넣는다.

사용법:
    python tools/build-proposal-docx.py <양식.docx> <proposal.md> <앱 이름> <출력.docx>

- proposal.md의 "## N." 절이 양식 02 파트의 N번 칸(1~8)에 들어간다.
- 지원 문법: ### 소제목, **굵게**, "- " 목록, "1. " 번호 목록, | 표 |, 빈 줄로 문단 구분,
  ![설명](images/파일.png) 그림(PNG, 경로는 proposal.md가 있는 폴더 기준. 설명은 그림 아래 작은 글씨).
- 양식의 "앱명 / 프로젝트명", "팀명" 빈칸도 채운다. 01 참가신청서의 개인정보와 04 서명은 손으로 채운다.
"""
import os
import re
import struct
import sys
import zipfile
from xml.sax.saxutils import escape

TEAM_NAME = "삼각고래"

# 양식 02 파트 각 칸의 콘텐츠 컨트롤 태그(양식 1020 ver. 기준, 순서대로 1~8번 칸)
SECTION_TAGS = {
    1: "app_summary",
    2: "planning_background",
    3: "vehicle_scenario",
    4: "key_features",
    5: "pleos_plan",
    6: "reference_files",
    7: "reference_link",
    8: "shortform_link",
}

CELL_WIDTH = 9600  # 양식 칸 너비(약 9,860 dxa)에서 셀 여백을 뺀 값
IMAGE_WIDTH = 9000  # 그림 너비(dxa). 칸 안에서 좌우 여백을 조금 둔다
IMAGE_RE = re.compile(r"^!\[(.*)\]\(([^()]+)\)\s*$")  # 설명은 탐욕적, 경로엔 괄호 없음
CAPTION_COLOR = "5A6B7B"
# 그림 상태: main()이 채운다. media = [(zip 안 이름, bytes)], rels = 추가할 Relationship XML 목록
IMG = {"dir": ".", "next_rid": 1, "next_id": 1, "media": [], "rels": []}
HEADING_COLOR = "16324F"
HEADER_FILL = "E8EEF4"
BORDER_COLOR = "8394A5"


def parse_sections(md_text):
    md_text = re.sub(r"<!--.*?-->", "", md_text, flags=re.S)
    sections, current = {}, None
    for line in md_text.splitlines():
        m = re.match(r"^## (\d+)\.", line)
        if m:
            current = int(m.group(1))
            sections[current] = []
        elif current is not None:
            sections[current].append(line.rstrip())
    return sections


def runs(text, size=None, bold=False):
    """**굵게**를 나눠 w:r 목록으로 만든다."""
    out = []
    for i, part in enumerate(re.split(r"\*\*(.+?)\*\*", text)):
        if not part:
            continue
        rpr = ""
        if bold or i % 2 == 1:
            rpr += "<w:b/><w:bCs/>"
        if size:
            rpr += f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>'
        rpr += '<w:lang w:eastAsia="ko-KR"/>'
        out.append(f'<w:r><w:rPr>{rpr}</w:rPr><w:t xml:space="preserve">{escape(part)}</w:t></w:r>')
    return "".join(out)


def para(text, *, before=0, after=60, indent=None, size=None, bold=False, color=None, keep_next=False):
    ppr = ""
    if keep_next:
        ppr += "<w:keepNext/>"
    ppr += f'<w:spacing w:before="{before}" w:after="{after}" w:line="276" w:lineRule="auto"/>'
    if indent:
        left, hanging = indent
        ppr += f'<w:ind w:left="{left}" w:hanging="{hanging}"/>'
    body = runs(text, size=size, bold=bold)
    if color:
        body = body.replace("<w:rPr>", f'<w:rPr><w:color w:val="{color}"/>')
    return f"<w:p><w:pPr>{ppr}</w:pPr>{body}</w:p>"


def table(rows):
    cells = [[c.strip() for c in r.strip().strip("|").split("|")] for r in rows]
    cells = [r for r in cells if not all(re.fullmatch(r":?-{3,}:?", c) for c in r)]
    ncol = max(len(r) for r in cells)
    for r in cells:
        r.extend([""] * (ncol - len(r)))
    # 열 너비: 글자 수에 비례, 최소 1,000 dxa
    weights = [max(4, min(40, max(len(r[i]) for r in cells))) for i in range(ncol)]
    widths = [max(1000, int(CELL_WIDTH * w / sum(weights))) for w in weights]
    widths[-1] += CELL_WIDTH - sum(widths)
    border = "".join(
        f'<w:{side} w:val="single" w:sz="4" w:space="0" w:color="{BORDER_COLOR}"/>'
        for side in ("top", "left", "bottom", "right", "insideH", "insideV")
    )
    xml = [
        f'<w:tbl><w:tblPr><w:tblW w:w="{CELL_WIDTH}" w:type="dxa"/><w:tblBorders>{border}</w:tblBorders>'
        '<w:tblLayout w:type="fixed"/><w:tblCellMar><w:top w:w="40" w:type="dxa"/><w:left w:w="80" w:type="dxa"/>'
        '<w:bottom w:w="40" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>'
        + "".join(f'<w:gridCol w:w="{w}"/>' for w in widths)
        + "</w:tblGrid>"
    ]
    for ri, r in enumerate(cells):
        header = ri == 0
        trpr = "<w:trPr><w:tblHeader/></w:trPr>" if header else ""
        xml.append(f"<w:tr>{trpr}")
        for ci, c in enumerate(r):
            shade = f'<w:shd w:val="clear" w:color="auto" w:fill="{HEADER_FILL}"/>' if header else ""
            xml.append(
                f'<w:tc><w:tcPr><w:tcW w:w="{widths[ci]}" w:type="dxa"/>{shade}</w:tcPr>'
                + para(c, after=0, size=18, bold=header)
                + "</w:tc>"
            )
        xml.append("</w:tr>")
    xml.append("</w:tbl>")
    return "".join(xml)


def png_size(data):
    """PNG IHDR에서 (너비, 높이) 픽셀을 읽는다."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit("그림은 PNG만 지원한다")
    w, h = struct.unpack(">II", data[16:24])
    return w, h


def image_block(caption, rel_path):
    """그림 한 장(가운데 정렬, 칸 너비에 맞춤) + 설명 문단."""
    path = rel_path if os.path.isabs(rel_path) else os.path.join(IMG["dir"], rel_path)
    if not os.path.exists(path):
        raise SystemExit(f"그림 파일이 없음: {path}")
    data = open(path, "rb").read()
    w, h = png_size(data)
    cx = IMAGE_WIDTH * 635  # dxa → EMU
    cy = int(cx * h / w)
    n = IMG["next_id"]
    rid = f"rId{IMG['next_rid']}"
    IMG["next_id"] += 1
    IMG["next_rid"] += 1
    name = f"media/proposal{n}.png"
    IMG["media"].append((f"word/{name}", data))
    IMG["rels"].append(
        f'<Relationship Id="{rid}" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" '
        f'Target="{name}"/>'
    )
    a_ns = "http://schemas.openxmlformats.org/drawingml/2006/main"
    pic_ns = "http://schemas.openxmlformats.org/drawingml/2006/picture"
    drawing = (
        '<w:p><w:pPr><w:keepNext/><w:jc w:val="center"/>'
        '<w:spacing w:before="80" w:after="20"/></w:pPr><w:r><w:drawing>'
        f'<wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/>'
        f'<wp:docPr id="{900 + n}" name="그림 {n}" descr="{escape(caption)}"/>'
        f'<wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="{a_ns}" noChangeAspect="1"/></wp:cNvGraphicFramePr>'
        f'<a:graphic xmlns:a="{a_ns}"><a:graphicData uri="{pic_ns}">'
        f'<pic:pic xmlns:pic="{pic_ns}"><pic:nvPicPr><pic:cNvPr id="{900 + n}" name="proposal{n}.png"/>'
        '<pic:cNvPicPr/></pic:nvPicPr>'
        f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
        f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>'
        '</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>'
    )
    out = drawing
    if caption:
        cap = para(caption, after=100, size=16, color=CAPTION_COLOR)
        out += cap.replace("<w:pPr>", '<w:pPr><w:jc w:val="center"/>', 1)
    return out


def section_xml(lines):
    out, i = [], 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith("|"):
            block = []
            while i < len(lines) and lines[i].startswith("|"):
                block.append(lines[i])
                i += 1
            out.append(table(block))
            out.append(para("", after=0, size=8))
            continue
        m = IMAGE_RE.match(line)
        if m:
            out.append(image_block(m.group(1).strip(), m.group(2).strip()))
            i += 1
            continue
        if line.startswith("### "):
            out.append(para(line[4:], before=160, after=60, bold=True, color=HEADING_COLOR))
        elif line.startswith("- "):
            out.append(para("• " + line[2:], after=40, indent=(360, 200)))
        elif re.match(r"^\d+\. ", line):
            out.append(para(line, after=40, indent=(360, 280)))
        elif line == "출처":
            out.append(para(line, before=120, after=20, size=18, bold=True, keep_next=True))
        else:
            out.append(para(line, after=80))
        i += 1
    return "".join(out) or para("")


def fill_blank(doc, label, value):
    """label이 들어 있는 문단의 밑줄(____) 빈칸을 value로 바꾼다."""

    def repl(m):
        p = m.group(0)
        text = "".join(re.findall(r"<w:t[^>]*>([^<]*)</w:t>", p))
        if not re.search(re.escape(label) + r"[^:]*:\s*_", text):
            return p
        done = [False]

        def t_repl(tm):
            content = tm.group(2)
            if "_" not in content:
                return tm.group(0)
            if not done[0]:
                done[0] = True
                content = re.sub(r"_+", " " + escape(value), content, count=1)
            content = content.replace("_", "")
            return f'{tm.group(1)}{content}</w:t>'

        return re.sub(r"(<w:t[^>]*>)([^<]*)</w:t>", t_repl, p)

    return re.sub(r"<w:p[ >].*?</w:p>", repl, doc, flags=re.S)


def replace_box(doc, tag, content_xml):
    pos = doc.find(f'<w:tag w:val="{tag}"/>')
    if pos < 0:
        raise SystemExit(f"양식에서 칸을 찾지 못함: {tag}")
    p_start = max(doc.rfind("<w:p ", 0, pos), doc.rfind("<w:p>", 0, pos))
    p_end = doc.find("</w:p>", pos) + len("</w:p>")
    doc = doc[:p_start] + content_xml + doc[p_end:]
    # 내용이 길면 칸이 여러 쪽에 걸치도록 '행 나눔 금지'를 푼다
    tbl_start = doc.rfind("<w:tbl>", 0, p_start)
    head = doc[tbl_start:p_start].replace("<w:cantSplit/>", "")
    return doc[:tbl_start] + head + doc[p_start:]


def main():
    if len(sys.argv) != 5:
        raise SystemExit(__doc__)
    template, md_path, app_name, out_path = sys.argv[1:]
    sections = parse_sections(open(md_path, encoding="utf-8").read())
    missing = sorted(set(SECTION_TAGS) - set(sections))
    if missing:
        raise SystemExit(f"proposal.md에 없는 절: {missing}")

    with zipfile.ZipFile(template) as zin:
        items = {n: zin.read(n) for n in zin.namelist()}
    doc = items["word/document.xml"].decode("utf-8")
    rels = items["word/_rels/document.xml.rels"].decode("utf-8")
    IMG["dir"] = os.path.dirname(os.path.abspath(md_path))
    IMG["next_rid"] = max([int(x) for x in re.findall(r'Id="rId(\d+)"', rels)] + [0]) + 1

    for num, tag in SECTION_TAGS.items():
        doc = replace_box(doc, tag, section_xml(sections[num]))
    doc = fill_blank(doc, "프로젝트명", app_name)
    doc = fill_blank(doc, "팀명", TEAM_NAME)

    items["word/document.xml"] = doc.encode("utf-8")
    if IMG["media"]:
        rels = rels.replace("</Relationships>", "".join(IMG["rels"]) + "</Relationships>")
        items["word/_rels/document.xml.rels"] = rels.encode("utf-8")
        ct = items["[Content_Types].xml"].decode("utf-8")
        if 'Extension="png"' not in ct:
            ct = ct.replace("<Default ", '<Default Extension="png" ContentType="image/png"/><Default ', 1)
            items["[Content_Types].xml"] = ct.encode("utf-8")
        for name, data in IMG["media"]:
            items[name] = data
    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as zout:
        for name, data in items.items():
            zout.writestr(name, data)
    print(f"작성됨: {out_path} (그림 {len(IMG['media'])}장)")


if __name__ == "__main__":
    main()
