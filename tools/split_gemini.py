#!/usr/bin/env python3
"""Tách câu trả lời của Gemini thành từng file trong project.

Cách dùng (chạy ở thư mục gốc repo):
    python tools/split_gemini.py docs/gemini/05-ca-nhan.md            # xem trước, không ghi gì
    python tools/split_gemini.py docs/gemini/05-ca-nhan.md --write    # ghi file mới
    python tools/split_gemini.py docs/gemini/05-ca-nhan.md --write --force   # ghi đè cả file đã có (cẩn thận)

Quy ước: mỗi khối code ```...``` có dòng đầu là `// FILE: <đường dẫn>` hoặc `<!-- FILE: <đường dẫn> -->`.
Mặc định KHÔNG ghi đè file đã tồn tại, để Gemini không xoá mất phần người khác đã sửa.
"""
import os
import re
import sys

FENCE = re.compile(r"```[\w+-]*\n(.*?)\n```", re.S)
MARK = re.compile(r"^\s*(?://|<!--)\s*FILE:\s*(\S+?)\s*(?:-->)?\s*$")


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    src_path = sys.argv[1]
    write = "--write" in sys.argv
    force = "--force" in sys.argv
    text = open(src_path, encoding="utf-8").read()
    new, skipped, extra = [], [], []
    segments = []  # (path, nội dung) — một khối code có thể chứa nhiều dòng FILE
    for block in FENCE.findall(text):
        current, buf, lead = None, [], []
        for line in block.split("\n"):
            m = MARK.match(line)
            if m:
                if current:
                    segments.append((current, buf))
                current, buf = m.group(1), []
            elif current:
                buf.append(line)
            else:
                lead.append(line)
        if current:
            segments.append((current, buf))
        elif "".join(lead).strip():
            extra.append(block)
    for path, lines in segments:
        # Bỏ các dòng chú thích/trống ở cuối (vạch ngăn "// ====", ghi chú cấu hình) — Gemini hay chèn giữa các file.
        while lines and (not lines[-1].strip() or lines[-1].lstrip().startswith("//")):
            tail_comment = lines.pop()
            if "AppNavHost" in tail_comment or "import" in tail_comment:
                extra.append(tail_comment)
        while lines and re.match(r"^\s*//\s*=+\s*$", lines[0]):
            lines.pop(0)  # vạch ngăn "// ====" ngay sau dòng FILE
        rest = "\n".join(lines)
        if not path.startswith("app/"):
            print(f"  ! bỏ qua đường dẫn lạ: {path}")
            continue
        exists = os.path.exists(path)
        if exists and not force:
            skipped.append(path)
            continue
        new.append(path)
        if write:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(rest.lstrip("\n").rstrip() + "\n")
    label = "ĐÃ GHI" if write else "SẼ GHI (chạy thêm --write để ghi)"
    print(f"{label}: {len(new)} file")
    for p in new:
        print("  +", p)
    if skipped:
        print(f"BỎ QUA vì đã tồn tại ({len(skipped)}), cần gộp tay hoặc dùng --force:")
        for p in skipped:
            print("  =", p)
    if extra:
        print(f"Khối không có dòng FILE ({len(extra)}), thường là hướng dẫn nối AppNavHost:")
        for b in extra:
            print("  ---\n  " + b[:400].replace("\n", "\n  "))
    return 0


if __name__ == "__main__":
    sys.exit(main())
