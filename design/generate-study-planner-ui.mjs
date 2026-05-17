import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const out = join(__dirname, "study-planner-mobile-ui.svg");

const W = 390;
const H = 844;
const GAP_X = 64;
const GAP_Y = 94;
const LABEL_H = 36;
const COLS = 4;
const BOARD_W = COLS * W + (COLS - 1) * GAP_X + 120;
const ROW_H = H + LABEL_H + GAP_Y;

const c = {
  canvas: "#F4E7CE",
  paper: "#FBF3E2",
  paper2: "#FFF9EA",
  ink: "#3F3831",
  muted: "#7E7064",
  line: "#E4D6BE",
  shadow: "#D3BE9E",
  yellow: "#FFE8A8",
  yellow2: "#FFF1C8",
  pink: "#FFD8DF",
  mint: "#CAEEDB",
  lavender: "#DED9FF",
  blue: "#CFEAF8",
  cream: "#FFF8E8",
  coral: "#FFBCB3",
  green: "#8ED8B5",
  purple: "#A89CE8",
  orange: "#F6B166",
  red: "#E96C61",
  white: "#FFFFFF",
};

function esc(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function attrs(o) {
  return Object.entries(o)
    .filter(([, v]) => v !== undefined && v !== null && v !== false)
    .map(([k, v]) => `${k}="${esc(v)}"`)
    .join(" ");
}

function el(name, props = {}, body = "") {
  return `<${name} ${attrs(props)}>${body}</${name}>`;
}

function rect(x, y, w, h, fill, opt = {}) {
  return `<rect ${attrs({ x, y, width: w, height: h, fill, ...opt })}/>`;
}

function circle(cx, cy, r, fill, opt = {}) {
  return `<circle ${attrs({ cx, cy, r, fill, ...opt })}/>`;
}

function line(x1, y1, x2, y2, opt = {}) {
  return `<line ${attrs({ x1, y1, x2, y2, ...opt })}/>`;
}

function path(d, opt = {}) {
  return `<path ${attrs({ d, ...opt })}/>`;
}

function text(x, y, t, opt = {}) {
  const base = {
    x,
    y,
    fill: opt.fill ?? c.ink,
    "font-family": opt.family ?? "Nunito, Quicksand, Arial, sans-serif",
    "font-size": opt.size ?? 14,
    "font-weight": opt.weight ?? 500,
    "letter-spacing": 0,
  };
  const extra = { ...opt };
  delete extra.size;
  delete extra.weight;
  delete extra.fill;
  delete extra.family;
  return el("text", { ...base, ...extra }, esc(t));
}

function multiline(x, y, lines, opt = {}) {
  return lines.map((l, i) => text(x, y + i * (opt.leading ?? 18), l, opt)).join("");
}

function card(x, y, w, h, fill = c.paper2, opt = {}) {
  return rect(x, y, w, h, fill, {
    rx: opt.rx ?? 22,
    stroke: opt.stroke ?? c.line,
    "stroke-width": opt.sw ?? 1.2,
    filter: "url(#softShadow)",
    ...opt.extra,
  });
}

function tape(x, y, w = 62, fill = c.yellow) {
  return `<g opacity=".9">${rect(x, y, w, 18, fill, { rx: 4, transform: `rotate(-5 ${x + w / 2} ${y + 9})` })}</g>`;
}

function highlight(x, y, w, h, fill = c.yellow) {
  return rect(x, y, w, h, fill, { rx: h / 2, opacity: 0.72 });
}

function pill(x, y, label, fill, w = 76, opt = {}) {
  return `${rect(x, y, w, 30, fill, { rx: 15, stroke: opt.stroke ?? "none" })}${text(x + w / 2, y + 20, label, { size: opt.size ?? 12, weight: 800, fill: opt.fill ?? c.ink, "text-anchor": "middle" })}`;
}

function button(x, y, w, h, label, fill = c.ink, opt = {}) {
  return `${rect(x, y, w, h, fill, { rx: h / 2, filter: "url(#softShadow)" })}${text(x + w / 2, y + h / 2 + 5, label, { size: opt.size ?? 15, weight: 900, fill: opt.text ?? c.white, "text-anchor": "middle" })}`;
}

function input(x, y, w, label, value = "", opt = {}) {
  return `${text(x, y, label, { size: 12, weight: 800, fill: c.muted })}${rect(x, y + 9, w, opt.h ?? 48, c.white, { rx: 16, stroke: c.line })}${value ? text(x + 16, y + 39, value, { size: 14, weight: 700, fill: opt.valueFill ?? c.ink }) : ""}`;
}

function checkbox(x, y, checked = false, fill = c.white) {
  return `${rect(x, y, 22, 22, fill, { rx: 7, stroke: checked ? c.green : c.line, "stroke-width": 1.8 })}${checked ? path(`M${x + 5} ${y + 11} L${x + 10} ${y + 16} L${x + 18} ${y + 6}`, { fill: "none", stroke: c.green, "stroke-width": 2.5, "stroke-linecap": "round", "stroke-linejoin": "round" }) : ""}`;
}

function icon(name, x, y, size = 24, stroke = c.ink) {
  const s = size / 24;
  const common = { fill: "none", stroke, "stroke-width": 2, "stroke-linecap": "round", "stroke-linejoin": "round", transform: `translate(${x} ${y}) scale(${s})` };
  const p = [];
  if (name === "calendar") {
    p.push(rect(3, 5, 18, 16, "none", { rx: 4, stroke, "stroke-width": 2, transform: `translate(${x} ${y}) scale(${s})` }));
    p.push(line(7, 3, 7, 7, common));
    p.push(line(17, 3, 17, 7, common));
    p.push(line(3, 10, 21, 10, common));
  } else if (name === "check") {
    p.push(path("M4 12.5l5 5L20 6", common));
  } else if (name === "clock") {
    p.push(circle(12, 12, 9, "none", { stroke, "stroke-width": 2, transform: `translate(${x} ${y}) scale(${s})` }));
    p.push(path("M12 7v6l4 2", common));
  } else if (name === "chart") {
    p.push(path("M5 19V11M12 19V5M19 19v-8", common));
  } else if (name === "camera") {
    p.push(rect(3, 7, 18, 13, "none", { rx: 4, stroke, "stroke-width": 2, transform: `translate(${x} ${y}) scale(${s})` }));
    p.push(circle(12, 13.5, 3.2, "none", { stroke, "stroke-width": 2, transform: `translate(${x} ${y}) scale(${s})` }));
    p.push(path("M8 7l1.5-2h5L16 7", common));
  } else if (name === "home") {
    p.push(path("M4 11.5L12 5l8 6.5V20H6V11.5", common));
  } else if (name === "plus") {
    p.push(line(12, 5, 12, 19, common));
    p.push(line(5, 12, 19, 12, common));
  } else if (name === "settings") {
    p.push(circle(12, 12, 3.4, "none", { stroke, "stroke-width": 2, transform: `translate(${x} ${y}) scale(${s})` }));
    p.push(path("M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1", common));
  } else if (name === "book") {
    p.push(path("M5 5h9a4 4 0 0 1 4 4v12H9a4 4 0 0 0-4-4V5z", common));
    p.push(path("M5 17a4 4 0 0 1 4-4h9", common));
  } else if (name === "trash") {
    p.push(path("M4 7h16M9 7V5h6v2M8 10v9M16 10v9M6 7l1 14h10l1-14", common));
  } else if (name === "edit") {
    p.push(path("M5 18.5V21h2.5L18.8 9.7l-2.5-2.5L5 18.5zM15.5 8l2.5-2.5 2.5 2.5L18 10.5", common));
  } else if (name === "alert") {
    p.push(path("M12 4l9 16H3L12 4z", common));
    p.push(line(12, 10, 12, 14, common));
    p.push(circle(12, 17, 0.8, stroke, { transform: `translate(${x} ${y}) scale(${s})` }));
  } else {
    p.push(circle(12, 12, 8, "none", { stroke, "stroke-width": 2, transform: `translate(${x} ${y}) scale(${s})` }));
  }
  return `<g>${p.join("")}</g>`;
}

function logo(x, y, scale = 1) {
  return `<g id="logo" transform="translate(${x} ${y}) scale(${scale})">
    ${rect(0, 0, 62, 62, c.yellow, { rx: 18, filter: "url(#softShadow)" })}
    ${rect(14, 14, 34, 40, c.paper2, { rx: 6, stroke: c.ink, "stroke-width": 2 })}
    ${line(21, 25, 41, 25, { stroke: c.purple, "stroke-width": 3, "stroke-linecap": "round" })}
    ${line(21, 34, 37, 34, { stroke: c.green, "stroke-width": 3, "stroke-linecap": "round" })}
    ${path("M20 45l6 5 15-18", { fill: "none", stroke: c.red, "stroke-width": 3, "stroke-linecap": "round", "stroke-linejoin": "round" })}
  </g>`;
}

function phone(title, row, col, body, opt = {}) {
  const x = 60 + col * (W + GAP_X);
  const y = 62 + row * ROW_H + LABEL_H;
  return `<g id="${esc(title).replaceAll(" ", "_")}">
    ${text(x, y - 13, title, { size: 18, weight: 900, fill: c.ink })}
    ${rect(x, y, W, H, opt.bg ?? c.paper, { rx: 34, stroke: "#CBB99C", "stroke-width": 2, filter: "url(#frameShadow)" })}
    ${rect(x + 132, y + 13, 126, 5, "#DCCFB8", { rx: 3, opacity: .8 })}
    ${body(x, y)}
  </g>`;
}

function header(x, y, title, subtitle = "", rightIcon = "settings") {
  return `<g id="header">
    ${text(x + 24, y + 56, title, { size: 24, weight: 900 })}
    ${subtitle ? text(x + 24, y + 80, subtitle, { size: 13, weight: 700, fill: c.muted }) : ""}
    ${rect(x + 328, y + 38, 38, 38, c.paper2, { rx: 14, stroke: c.line })}
    ${icon(rightIcon, x + 335, y + 45, 24, c.ink)}
  </g>`;
}

function bottomNav(x, y, active = 0) {
  const tabs = [
    ["Dashboard", "home"],
    ["Lịch", "calendar"],
    ["To-Do", "check"],
    ["Pomodoro", "clock"],
    ["Thống kê", "chart"],
  ];
  return `<g id="Bottom_Navigation">
    ${rect(x + 16, y + 760, 358, 64, c.paper2, { rx: 25, stroke: c.line, filter: "url(#softShadow)" })}
    ${tabs.map((t, i) => {
      const tx = x + 35 + i * 70;
      const activeFill = i === active ? c.yellow : "transparent";
      const color = i === active ? c.ink : c.muted;
      return `<g id="Tab_${esc(t[0])}">
        ${rect(tx - 7, y + 771, 56, 42, activeFill, { rx: 18, opacity: i === active ? 1 : 0 })}
        ${icon(t[1], tx + 9, y + 779, 20, color)}
        ${text(tx + 18, y + 808, t[0], { size: 9.5, weight: 900, fill: color, "text-anchor": "middle" })}
      </g>`;
    }).join("")}
  </g>`;
}

function fab(x, y, iconName = "plus", label = "") {
  return `<g id="Floating_Action_Button">
    ${rect(x, y, label ? 128 : 58, 58, c.ink, { rx: 29, filter: "url(#softShadow)" })}
    ${icon(iconName, x + 17, y + 17, 24, c.white)}
    ${label ? text(x + 49, y + 36, label, { size: 13, weight: 900, fill: c.white }) : ""}
  </g>`;
}

function miniChart(x, y, w = 150, h = 60) {
  const bars = [30, 48, 38, 55, 42, 62, 50];
  return `<g id="Weekly_Progress_Chart">
    ${bars.map((b, i) => rect(x + i * 20, y + h - b, 12, b, [c.yellow, c.pink, c.mint, c.lavender, c.blue, c.coral, c.yellow2][i], { rx: 6 })).join("")}
    ${text(x, y + h + 20, "T2  T3  T4  T5  T6  T7  CN", { size: 10, fill: c.muted, weight: 800 })}
  </g>`;
}

function taskRow(x, y, name, subject, due, priority, checked = false, fill = c.paper2) {
  const pcolor = priority === "Cao" ? c.coral : priority === "Trung bình" ? c.yellow : c.mint;
  return `<g id="Task_${esc(name)}">
    ${card(x, y, 342, 76, fill, { rx: 18 })}
    ${checkbox(x + 16, y + 24, checked)}
    ${text(x + 50, y + 28, name, { size: 15, weight: 900 })}
    ${text(x + 50, y + 50, `${subject} • ${due}`, { size: 12, weight: 700, fill: c.muted })}
    ${pill(x + 261, y + 22, priority, pcolor, priority === "Trung bình" ? 68 : 52, { size: 10 })}
  </g>`;
}

function eventBlock(x, y, w, h, title, time, fill, opt = {}) {
  return `<g id="Event_${esc(title)}">
    ${rect(x, y, w, h, fill, { rx: 14, stroke: opt.stroke ?? "none" })}
    ${text(x + 12, y + 23, title, { size: 13, weight: 900 })}
    ${text(x + 12, y + 43, time, { size: 11, weight: 800, fill: c.muted })}
  </g>`;
}

function uiKit(x, y) {
  return `<g id="UI_Kit_Board">
    ${text(x, y - 13, "00. UI kit Studygram", { size: 18, weight: 900 })}
    ${rect(x, y, W, H, c.paper, { rx: 34, stroke: "#CBB99C", "stroke-width": 2, filter: "url(#frameShadow)" })}
    ${text(x + 24, y + 54, "Study Planner UI Kit", { size: 25, weight: 900 })}
    ${text(x + 24, y + 78, "Màu giấy ngà, pastel note, icon nét mềm", { size: 12.5, weight: 700, fill: c.muted })}
    ${logo(x + 298, y + 35, .72)}
    ${text(x + 24, y + 122, "Color tokens", { size: 16, weight: 900 })}
    ${[
      ["Paper", c.paper], ["Card", c.paper2], ["Ink", c.ink], ["Yellow", c.yellow],
      ["Pink", c.pink], ["Mint", c.mint], ["Lavender", c.lavender], ["Blue", c.blue],
      ["Coral", c.coral], ["Line", c.line]
    ].map((sw, i) => `${rect(x + 24 + (i % 5) * 67, y + 142 + Math.floor(i / 5) * 70, 46, 46, sw[1], { rx: 14, stroke: c.line })}${text(x + 47 + (i % 5) * 67, y + 202 + Math.floor(i / 5) * 70, sw[0], { size: 10, weight: 800, fill: c.muted, "text-anchor": "middle" })}`).join("")}
    ${text(x + 24, y + 306, "Typography", { size: 16, weight: 900 })}
    ${text(x + 24, y + 342, "Heading 1 / Nunito ExtraBold", { size: 24, weight: 900 })}
    ${text(x + 24, y + 370, "Body text / Nunito Medium dễ đọc cho mobile", { size: 14, weight: 600, fill: c.muted })}
    ${text(x + 24, y + 414, "Components", { size: 16, weight: 900 })}
    ${button(x + 24, y + 435, 160, 48, "Bắt đầu")}
    ${button(x + 200, y + 435, 142, 48, "Google", c.white, { text: c.ink })}
    ${input(x + 24, y + 510, 318, "Input", "Nhập tên công việc")}
    ${card(x + 24, y + 594, 146, 92, c.yellow2)}${tape(x + 64, y + 584, 58, c.pink)}${text(x + 42, y + 631, "Card giấy note", { size: 14, weight: 900 })}${text(x + 42, y + 654, "Bo góc mềm", { size: 12, fill: c.muted, weight: 700 })}
    ${pill(x + 196, y + 600, "Cao", c.coral, 54)}${pill(x + 196, y + 640, "Trung bình", c.yellow, 86)}${pill(x + 196, y + 680, "Thấp", c.mint, 58)}
    ${rect(x + 24, y + 730, 342, 64, c.paper2, { rx: 25, stroke: c.line, filter: "url(#softShadow)" })}
    ${["home","calendar","check","clock","chart"].map((ic, i) => icon(ic, x + 49 + i * 70, y + 750, 24, i === 0 ? c.ink : c.muted)).join("")}
  </g>`;
}

const screens = [];

screens.push(uiKit(60, 62 + LABEL_H));

screens.push(phone("01. Splash / Onboarding", 0, 1, (x, y) => `
  ${highlight(x + 72, y + 126, 242, 24, c.yellow)}
  ${logo(x + 145, y + 128, 1.6)}
  ${text(x + 195, y + 284, "MANAGING YOUR", { size: 24, weight: 900, "text-anchor": "middle" })}
  ${text(x + 195, y + 318, "STUDY SCHEDULE", { size: 28, weight: 900, "text-anchor": "middle" })}
  ${multiline(x + 55, y + 376, ["Quản lý lịch học thông minh,", "học tập tập trung hơn"], { size: 16, weight: 700, fill: c.muted, "text-anchor": "start", leading: 24 })}
  ${card(x + 46, y + 456, 298, 158, c.paper2)}
  ${tape(x + 163, y + 444, 64, c.pink)}
  ${text(x + 76, y + 502, "Hôm nay học gì?", { size: 20, weight: 900 })}
  ${text(x + 76, y + 534, "Lịch học, deadline, Pomodoro", { size: 14, weight: 700, fill: c.muted })}
  ${miniChart(x + 78, y + 558, 150, 42)}
  ${button(x + 48, y + 704, 294, 54, "Bắt đầu")}
`));

screens.push(phone("02. Đăng nhập", 0, 2, (x, y) => `
  ${logo(x + 157, y + 56, 1.22)}
  ${text(x + 195, y + 179, "Chào mừng quay lại", { size: 25, weight: 900, "text-anchor": "middle" })}
  ${text(x + 195, y + 206, "Đăng nhập để tiếp tục quản lý lịch học của bạn", { size: 13, weight: 700, fill: c.muted, "text-anchor": "middle" })}
  ${input(x + 32, y + 260, 326, "Email / số điện thoại", "student@email.com")}
  ${input(x + 32, y + 342, 326, "Mật khẩu", "••••••••")}
  ${checkbox(x + 32, y + 422, true)}${text(x + 62, y + 438, "Ghi nhớ đăng nhập", { size: 13, weight: 700, fill: c.muted })}
  ${text(x + 251, y + 438, "Quên mật khẩu?", { size: 13, weight: 900, fill: c.red })}
  ${button(x + 32, y + 486, 326, 52, "Đăng nhập")}
  ${button(x + 32, y + 556, 326, 52, "Đăng nhập với Google", c.white, { text: c.ink })}
  ${text(x + 195, y + 680, "Chưa có tài khoản? Đăng ký ngay", { size: 14, weight: 800, fill: c.muted, "text-anchor": "middle" })}
`));

screens.push(phone("03. Đăng ký", 0, 3, (x, y) => `
  ${header(x, y, "Tạo tài khoản mới", "Bắt đầu xây lịch học của riêng bạn", "book")}
  ${input(x + 32, y + 128, 326, "Họ tên", "Nguyễn Minh Anh")}
  ${input(x + 32, y + 204, 326, "Email / số điện thoại", "student@email.com")}
  ${input(x + 32, y + 280, 326, "Mật khẩu", "••••••••")}
  ${input(x + 32, y + 356, 326, "Xác nhận mật khẩu", "••••••••")}
  ${checkbox(x + 32, y + 438, false)}${text(x + 62, y + 454, "Đồng ý điều khoản sử dụng", { size: 13, weight: 700, fill: c.muted })}
  ${button(x + 32, y + 500, 326, 52, "Đăng ký")}
  ${button(x + 32, y + 568, 326, 52, "Đăng ký với Google", c.white, { text: c.ink })}
  ${text(x + 195, y + 702, "Đã có tài khoản? Đăng nhập", { size: 14, weight: 800, fill: c.muted, "text-anchor": "middle" })}
`));

screens.push(phone("04. Quên mật khẩu", 1, 0, (x, y) => `
  ${header(x, y, "Quên mật khẩu", "Nhập email của bạn để nhận mã xác minh", "clock")}
  ${card(x + 32, y + 160, 326, 180, c.yellow2)}
  ${tape(x + 158, y + 149, 70, c.pink)}
  ${text(x + 62, y + 220, "Kiểm tra hộp thư", { size: 20, weight: 900 })}
  ${text(x + 62, y + 250, "Mã xác minh sẽ hết hạn sau vài phút.", { size: 13, weight: 700, fill: c.muted })}
  ${input(x + 32, y + 386, 326, "Email", "student@email.com")}
  ${button(x + 32, y + 482, 326, 52, "Gửi mã xác minh")}
  ${text(x + 195, y + 594, "Quay lại đăng nhập", { size: 14, weight: 900, fill: c.red, "text-anchor": "middle" })}
`));

screens.push(phone("05. Xác minh OTP", 1, 1, (x, y) => `
  ${header(x, y, "Xác minh tài khoản", "Nhập mã xác minh được gửi đến email của bạn", "check")}
  ${[0,1,2,3,4,5].map((i) => `${rect(x + 32 + i * 55, y + 198, 44, 56, c.white, { rx: 15, stroke: i < 4 ? c.ink : c.line, "stroke-width": i < 4 ? 1.8 : 1.2 })}${i < 4 ? text(x + 54 + i * 55, y + 234, String([2,8,4,6][i]), { size: 22, weight: 900, "text-anchor": "middle" }) : ""}`).join("")}
  ${card(x + 58, y + 302, 274, 72, c.paper2)}
  ${text(x + 195, y + 332, "Gửi lại mã sau", { size: 13, weight: 800, fill: c.muted, "text-anchor": "middle" })}
  ${text(x + 195, y + 358, "00:42", { size: 22, weight: 900, fill: c.red, "text-anchor": "middle" })}
  ${button(x + 32, y + 438, 326, 52, "Xác nhận")}
  ${text(x + 195, y + 540, "Gửi lại mã", { size: 14, weight: 900, fill: c.red, "text-anchor": "middle" })}
`));

screens.push(phone("06. Tạo mật khẩu mới", 1, 2, (x, y) => `
  ${header(x, y, "Tạo mật khẩu mới", "Cập nhật mật khẩu an toàn cho tài khoản", "settings")}
  ${input(x + 32, y + 180, 326, "Mật khẩu mới", "••••••••")}
  ${input(x + 32, y + 262, 326, "Xác nhận mật khẩu mới", "••••••••")}
  ${card(x + 32, y + 370, 326, 92, c.mint)}
  ${text(x + 60, y + 408, "Gợi ý", { size: 15, weight: 900 })}
  ${text(x + 60, y + 434, "Dùng ít nhất 8 ký tự, có số và chữ.", { size: 13, weight: 700, fill: c.muted })}
  ${button(x + 32, y + 522, 326, 52, "Cập nhật mật khẩu")}
`));

screens.push(phone("07. Thiết lập hồ sơ", 1, 3, (x, y) => `
  ${header(x, y, "Thiết lập hồ sơ", "Cá nhân hóa lịch học của bạn", "book")}
  ${input(x + 32, y + 116, 326, "Tên hiển thị", "Minh Anh")}
  ${text(x + 32, y + 220, "Vai trò", { size: 12, weight: 900, fill: c.muted })}
  ${pill(x + 32, y + 236, "Học sinh", c.white, 82, { stroke: c.line })}${pill(x + 122, y + 236, "Sinh viên", c.yellow, 88)}${pill(x + 218, y + 236, "Tự học", c.white, 82, { stroke: c.line })}
  ${text(x + 32, y + 304, "Mục tiêu học tập", { size: 12, weight: 900, fill: c.muted })}
  ${["Quản lý lịch học","Theo dõi deadline","Pomodoro tập trung","Thống kê tiến độ"].map((l, i) => `${checkbox(x + 32, y + 326 + i * 42, i !== 3)}${text(x + 66, y + 342 + i * 42, l, { size: 14, weight: 800 })}`).join("")}
  ${input(x + 32, y + 520, 326, "Khung giờ học thường xuyên", "19:00 - 21:30")}
  ${button(x + 32, y + 676, 326, 52, "Hoàn tất thiết lập")}
`));

screens.push(phone("08. Dashboard tổng quan", 2, 0, (x, y) => `
  ${header(x, y, "Chào Minh Anh", "Hôm nay bạn có 3 việc cần ưu tiên", "settings")}
  ${card(x + 24, y + 106, 342, 112, c.yellow2)}
  ${tape(x + 156, y + 96, 70, c.pink)}
  ${text(x + 48, y + 145, "Lịch học tiếp theo", { size: 13, weight: 900, fill: c.muted })}
  ${text(x + 48, y + 174, "Toán rời rạc", { size: 22, weight: 900 })}
  ${text(x + 48, y + 199, "09:30 - 11:30 • P. B203", { size: 13, weight: 800, fill: c.muted })}
  ${card(x + 24, y + 238, 162, 94, c.pink)}${text(x + 44, y + 275, "Deadline sắp đến", { size: 12, weight: 900, fill: c.muted })}${text(x + 44, y + 304, "Báo cáo UX", { size: 16, weight: 900 })}
  ${card(x + 204, y + 238, 162, 94, c.mint)}${text(x + 224, y + 275, "Công việc hôm nay", { size: 12, weight: 900, fill: c.muted })}${text(x + 224, y + 304, "5 task", { size: 19, weight: 900 })}
  ${card(x + 24, y + 354, 342, 114, c.paper2)}
  ${text(x + 46, y + 384, "Thống kê nhanh", { size: 15, weight: 900 })}
  ${text(x + 52, y + 425, "12", { size: 24, weight: 900 })}${text(x + 39, y + 446, "Hoàn thành", { size: 11, weight: 800, fill: c.muted })}
  ${text(x + 158, y + 425, "2", { size: 24, weight: 900, fill: c.red })}${text(x + 132, y + 446, "Quá hạn", { size: 11, weight: 800, fill: c.muted })}
  ${text(x + 260, y + 425, "8h", { size: 24, weight: 900 })}${text(x + 236, y + 446, "Tập trung", { size: 11, weight: 800, fill: c.muted })}
  ${card(x + 24, y + 492, 342, 118, c.paper2)}${text(x + 46, y + 525, "Tiến độ học tập tuần", { size: 15, weight: 900 })}${miniChart(x + 50, y + 546, 210, 42)}
  ${button(x + 24, y + 632, 105, 44, "Thêm lịch", c.yellow, { text: c.ink, size: 12 })}
  ${button(x + 142, y + 632, 95, 44, "Thêm việc", c.mint, { text: c.ink, size: 12 })}
  ${button(x + 250, y + 632, 116, 44, "Chụp ảnh", c.ink, { text: c.white, size: 12 })}
  ${bottomNav(x, y, 0)}
`));

screens.push(phone("09. Lịch học", 2, 1, (x, y) => `
  ${header(x, y, "Lịch học", "Xem theo ngày, tuần hoặc tháng", "calendar")}
  ${pill(x + 32, y + 104, "Ngày", c.yellow, 70)}${pill(x + 112, y + 104, "Tuần", c.white, 70, { stroke: c.line })}${pill(x + 192, y + 104, "Tháng", c.white, 76, { stroke: c.line })}
  ${card(x + 24, y + 152, 342, 448, c.paper2)}
  ${["T2","T3","T4","T5","T6","T7","CN"].map((d, i) => text(x + 54 + i * 45, y + 190, d, { size: 11, weight: 900, fill: c.muted, "text-anchor": "middle" })).join("")}
  ${["18","19","20","21","22","23","24"].map((d, i) => `${circle(x + 54 + i * 45, y + 220, 17, i === 2 ? c.yellow : c.cream, { stroke: c.line })}${text(x + 54 + i * 45, y + 225, d, { size: 12, weight: 900, "text-anchor": "middle" })}`).join("")}
  ${line(x + 48, y + 270, x + 342, y + 270, { stroke: c.line })}
  ${eventBlock(x + 72, y + 292, 236, 64, "Toán rời rạc", "09:30 - 11:30 • Lịch học", c.mint)}
  ${eventBlock(x + 72, y + 370, 236, 64, "Thi Cấu trúc dữ liệu", "13:00 - 14:30 • Lịch thi", c.pink)}
  ${eventBlock(x + 72, y + 448, 236, 64, "Nộp báo cáo UX", "20:00 • Deadline", c.yellow2)}
  ${rect(x + 42, y + 532, 306, 48, "#FFF0EE", { rx: 16, stroke: c.red })}
  ${icon("alert", x + 58, y + 544, 20, c.red)}${text(x + 88, y + 562, "Lịch này bị trùng với sự kiện khác", { size: 12.5, weight: 900, fill: c.red })}
  ${fab(x + 300, y + 674)}
  ${bottomNav(x, y, 1)}
`));

screens.push(phone("10. To-Do List học tập", 2, 2, (x, y) => `
  ${header(x, y, "To-Do List", "Sắp xếp việc học theo deadline", "check")}
  ${["Tất cả","Hôm nay","Sắp hạn","Quá hạn"].map((f, i) => pill(x + 24 + i * 84, y + 104, f, i === 0 ? c.yellow : c.white, i === 2 ? 78 : 72, { stroke: i === 0 ? "none" : c.line, size: 11 })).join("")}
  ${taskRow(x + 24, y + 160, "Làm bài tập Chương 4", "CSDL", "Hôm nay 21:00", "Cao")}
  ${taskRow(x + 24, y + 250, "Đọc tài liệu Kotlin", "Mobile", "Mai 08:00", "Trung bình")}
  ${taskRow(x + 24, y + 340, "Ôn tập kiểm tra", "Giải tích", "22/05", "Cao")}
  ${taskRow(x + 24, y + 430, "Tóm tắt bài giảng", "UX/UI", "Đã xong", "Thấp", true)}
  ${taskRow(x + 24, y + 520, "Nộp file nhóm", "CNPM", "Quá hạn", "Cao", false, "#FFF0EE")}
  ${fab(x + 276, y + 674, "plus", "Thêm task")}
  ${bottomNav(x, y, 2)}
`));

screens.push(phone("11. Thêm / Sửa công việc", 2, 3, (x, y) => `
  ${header(x, y, "Thêm công việc", "Tạo task học tập trong vài bước", "check")}
  ${input(x + 32, y + 112, 326, "Tên công việc", "Hoàn thành slide thuyết trình")}
  ${input(x + 32, y + 188, 326, "Môn học", "Thiết kế UI/UX")}
  ${input(x + 32, y + 264, 326, "Deadline", "20/05/2026 • 22:00")}
  ${text(x + 32, y + 368, "Mức độ ưu tiên", { size: 12, weight: 900, fill: c.muted })}
  ${pill(x + 32, y + 386, "Cao", c.coral, 58)}${pill(x + 104, y + 386, "Trung bình", c.white, 96, { stroke: c.line })}${pill(x + 216, y + 386, "Thấp", c.white, 66, { stroke: c.line })}
  ${input(x + 32, y + 458, 326, "Ghi chú", "Chuẩn bị hình minh họa và checklist", { h: 92 })}
  ${card(x + 32, y + 590, 326, 58, c.paper2)}
  ${text(x + 56, y + 625, "Nhắc nhở trước deadline", { size: 14, weight: 900 })}
  ${rect(x + 290, y + 606, 48, 26, c.mint, { rx: 13 })}${circle(x + 325, y + 619, 10, c.white)}
  ${button(x + 32, y + 704, 326, 52, "Lưu")}
`));

screens.push(phone("12. Tạo lịch từ hình ảnh", 3, 0, (x, y) => `
  ${header(x, y, "Chụp ảnh lịch", "Tự động tạo lịch từ thời khóa biểu", "camera")}
  ${card(x + 24, y + 104, 342, 204, c.paper2)}
  ${rect(x + 48, y + 130, 294, 146, c.cream, { rx: 20, stroke: c.line, "stroke-dasharray": "7 7" })}
  ${icon("camera", x + 173, y + 171, 42, c.muted)}
  ${text(x + 195, y + 239, "Chụp ảnh hoặc tải ảnh lịch thi", { size: 13, weight: 900, fill: c.muted, "text-anchor": "middle" })}
  ${button(x + 42, y + 326, 146, 44, "Chụp ảnh", c.ink, { size: 12 })}${button(x + 202, y + 326, 146, 44, "Tải ảnh", c.white, { text: c.ink, size: 12 })}
  ${card(x + 24, y + 394, 342, 74, c.yellow2)}
  ${circle(x + 56, y + 431, 13, "none", { stroke: c.orange, "stroke-width": 4, "stroke-dasharray": "20 12" })}
  ${text(x + 84, y + 435, "Đang nhận dạng nội dung...", { size: 14, weight: 900 })}
  ${card(x + 24, y + 492, 342, 144, c.paper2)}
  ${text(x + 48, y + 526, "Thông tin trích xuất", { size: 15, weight: 900 })}
  ${["Môn học: Lập trình Mobile","Ngày học: Thứ 4, 20/05","Giờ: 09:30 - 11:30","Phòng: B203"].map((l, i) => text(x + 48, y + 558 + i * 24, l, { size: 12.5, weight: 800, fill: c.muted })).join("")}
  ${button(x + 32, y + 700, 326, 52, "Tạo lịch")}
`));

screens.push(phone("13. Pomodoro học tập", 3, 1, (x, y) => `
  ${header(x, y, "Pomodoro", "Tập trung sâu, ít xao nhãng", "clock")}
  ${circle(x + 195, y + 250, 118, c.yellow2, { stroke: c.line, "stroke-width": 2, filter: "url(#softShadow)" })}
  ${circle(x + 195, y + 250, 96, "none", { stroke: c.green, "stroke-width": 10, "stroke-linecap": "round", "stroke-dasharray": "420 180", transform: `rotate(-90 ${x + 195} ${y + 250})` })}
  ${text(x + 195, y + 238, "25:00", { size: 50, weight: 900, "text-anchor": "middle" })}
  ${text(x + 195, y + 274, "Học tập", { size: 15, weight: 900, fill: c.muted, "text-anchor": "middle" })}
  ${button(x + 52, y + 410, 96, 46, "Bắt đầu", c.ink, { size: 12 })}${button(x + 160, y + 410, 80, 46, "Tạm dừng", c.white, { text: c.ink, size: 12 })}${button(x + 252, y + 410, 86, 46, "Đặt lại", c.white, { text: c.ink, size: 12 })}
  ${input(x + 32, y + 492, 326, "Task / môn học đang học", "Ôn kiểm tra CSDL")}
  ${card(x + 32, y + 584, 152, 78, c.mint)}${text(x + 56, y + 619, "4 phiên", { size: 22, weight: 900 })}${text(x + 56, y + 642, "hoàn thành hôm nay", { size: 11, weight: 800, fill: c.muted })}
  ${card(x + 204, y + 584, 154, 78, c.pink)}${text(x + 228, y + 619, "5 phút", { size: 22, weight: 900 })}${text(x + 228, y + 642, "nghỉ sau phiên này", { size: 11, weight: 800, fill: c.muted })}
  ${card(x + 32, y + 680, 326, 58, c.paper2)}${text(x + 56, y + 715, "Bạn đang đi đúng nhịp. Hoàn thành từng phiên nhỏ nhé.", { size: 12.5, weight: 800, fill: c.muted })}
  ${bottomNav(x, y, 3)}
`));

screens.push(phone("14. Báo cáo thống kê", 3, 2, (x, y) => `
  ${header(x, y, "Thống kê", "Nhìn nhanh tiến độ học tập", "chart")}
  ${pill(x + 30, y + 104, "Ngày", c.white, 66, { stroke: c.line })}${pill(x + 106, y + 104, "Tuần", c.yellow, 66)}${pill(x + 182, y + 104, "Tháng", c.white, 72, { stroke: c.line })}
  ${card(x + 24, y + 152, 342, 128, c.paper2)}${text(x + 48, y + 184, "Thời gian tập trung", { size: 15, weight: 900 })}${miniChart(x + 56, y + 202, 210, 54)}
  ${card(x + 24, y + 302, 160, 126, c.paper2)}
  ${text(x + 48, y + 332, "Hoàn thành", { size: 14, weight: 900 })}
  ${circle(x + 104, y + 377, 34, c.mint)}${path(`M${x + 104} ${y + 377} L${x + 104} ${y + 343} A34 34 0 0 1 ${x + 135} ${y + 391} Z`, { fill: c.coral })}
  ${text(x + 104, y + 383, "78%", { size: 15, weight: 900, "text-anchor": "middle" })}
  ${card(x + 204, y + 302, 162, 126, c.paper2)}
  ${text(x + 226, y + 332, "Deadline / môn", { size: 14, weight: 900 })}
  ${[42,68,34,55].map((bh, i) => rect(x + 230 + i * 28, y + 402 - bh, 16, bh, [c.pink,c.yellow,c.blue,c.lavender][i], { rx: 8 })).join("")}
  ${card(x + 24, y + 452, 342, 84, c.yellow2)}${text(x + 48, y + 486, "Task quá hạn", { size: 13, weight: 900, fill: c.muted })}${text(x + 48, y + 516, "2 task", { size: 24, weight: 900, fill: c.red })}${text(x + 184, y + 502, "Môn nhiều deadline nhất: CSDL", { size: 13, weight: 900 })}
  ${card(x + 24, y + 560, 342, 112, c.paper2)}${text(x + 48, y + 592, "Heatmap hoạt động", { size: 15, weight: 900 })}
  ${Array.from({length: 35}, (_, i) => rect(x + 48 + (i % 7) * 34, y + 614 + Math.floor(i / 7) * 14, 20, 10, [c.cream,c.mint,c.yellow,c.pink][(i * 7 + i) % 4], { rx: 4, stroke: c.line, "stroke-width": .4 })).join("")}
  ${bottomNav(x, y, 4)}
`));

screens.push(phone("15. Chi tiết sự kiện", 3, 3, (x, y) => `
  ${header(x, y, "Chi tiết sự kiện", "Thông tin lịch học và nhắc nhở", "calendar")}
  ${card(x + 32, y + 118, 326, 142, c.yellow2)}
  ${tape(x + 157, y + 106, 70, c.pink)}
  ${text(x + 58, y + 164, "Thi Cấu trúc dữ liệu", { size: 24, weight: 900 })}
  ${pill(x + 58, y + 190, "Lịch thi", c.pink, 74)}
  ${card(x + 32, y + 290, 326, 240, c.paper2)}
  ${["Thời gian: 13:00 - 14:30, 22/05/2026","Địa điểm: Phòng A405","Môn học: Cấu trúc dữ liệu","Ghi chú: Mang thẻ sinh viên","Nhắc nhở: Bật, trước 30 phút"].map((l, i) => text(x + 58, y + 330 + i * 38, l, { size: 14, weight: 800, fill: i === 0 ? c.ink : c.muted })).join("")}
  ${button(x + 32, y + 590, 152, 52, "Sửa", c.ink)}${button(x + 206, y + 590, 152, 52, "Xóa", c.coral, { text: c.ink })}
`));

screens.push(phone("16. Cài đặt", 4, 0, (x, y) => `
  ${header(x, y, "Cài đặt", "Tùy chỉnh trải nghiệm học tập", "settings")}
  ${["Thông báo nhắc nhở","Đồng bộ Google Calendar","Chế độ offline","Quản lý môn học","Giao diện sáng / tối","Quản lý tài khoản"].map((l, i) => `${card(x + 24, y + 116 + i * 82, 342, 62, i === 5 ? c.yellow2 : c.paper2, { rx: 18 })}${text(x + 54, y + 154 + i * 82, l, { size: 15, weight: 900 })}${text(x + 334, y + 154 + i * 82, "›", { size: 24, weight: 900, fill: c.muted, "text-anchor": "middle" })}`).join("")}
`));

screens.push(phone("17. Quản lý tài khoản", 4, 1, (x, y) => `
  ${header(x, y, "Tài khoản", "Thông tin cá nhân và bảo mật", "settings")}
  ${circle(x + 195, y + 156, 44, c.yellow, { stroke: c.line, "stroke-width": 2 })}
  ${text(x + 195, y + 166, "MA", { size: 24, weight: 900, "text-anchor": "middle" })}
  ${text(x + 195, y + 226, "Nguyễn Minh Anh", { size: 22, weight: 900, "text-anchor": "middle" })}
  ${text(x + 195, y + 252, "student@email.com", { size: 13, weight: 700, fill: c.muted, "text-anchor": "middle" })}
  ${card(x + 32, y + 292, 326, 66, c.mint)}${text(x + 58, y + 332, "Trạng thái đồng bộ: Đang bật", { size: 15, weight: 900 })}
  ${card(x + 32, y + 386, 326, 66, c.paper2)}${text(x + 58, y + 426, "Đổi mật khẩu", { size: 15, weight: 900 })}
  ${card(x + 32, y + 468, 326, 66, c.paper2)}${text(x + 58, y + 508, "Đăng xuất", { size: 15, weight: 900 })}
  ${card(x + 32, y + 550, 326, 66, "#FFF0EE")}${text(x + 58, y + 590, "Xóa tài khoản", { size: 15, weight: 900, fill: c.red })}
`));

screens.push(phone("18. Thêm lịch / sự kiện", 4, 2, (x, y) => `
  ${header(x, y, "Thêm lịch", "Tạo lịch học, lịch thi hoặc deadline", "calendar")}
  ${input(x + 32, y + 112, 326, "Tên sự kiện", "Thực hành Lập trình Mobile")}
  ${text(x + 32, y + 216, "Loại sự kiện", { size: 12, weight: 900, fill: c.muted })}
  ${pill(x + 32, y + 234, "Lịch học", c.mint, 78)}${pill(x + 120, y + 234, "Lịch thi", c.white, 72, { stroke: c.line })}${pill(x + 202, y + 234, "Deadline", c.white, 84, { stroke: c.line })}
  ${input(x + 32, y + 300, 326, "Môn học", "Lập trình Mobile")}
  ${input(x + 32, y + 376, 150, "Ngày", "20/05/2026")}
  ${input(x + 204, y + 376, 154, "Phòng học", "B203")}
  ${input(x + 32, y + 452, 150, "Bắt đầu", "09:30")}
  ${input(x + 204, y + 452, 154, "Kết thúc", "11:30")}
  ${input(x + 32, y + 528, 326, "Ghi chú", "Mang laptop và tài liệu nhóm", { h: 70 })}
  ${rect(x + 32, y + 628, 326, 48, "#FFF0EE", { rx: 16, stroke: c.red })}
  ${icon("alert", x + 48, y + 640, 20, c.red)}${text(x + 78, y + 658, "Lịch này bị trùng với sự kiện khác", { size: 12.5, weight: 900, fill: c.red })}
  ${button(x + 32, y + 704, 326, 52, "Lưu lịch")}
`));

screens.push(phone("19. Xác nhận thông tin OCR", 4, 3, (x, y) => `
  ${header(x, y, "Xác nhận thông tin", "Chỉnh sửa trước khi lưu vào lịch", "check")}
  ${card(x + 24, y + 104, 342, 108, c.paper2)}
  ${rect(x + 46, y + 126, 98, 64, c.cream, { rx: 14, stroke: c.line, "stroke-dasharray": "5 5" })}
  ${icon("camera", x + 83, y + 145, 24, c.muted)}
  ${text(x + 164, y + 146, "Ảnh thời khóa biểu", { size: 15, weight: 900 })}
  ${text(x + 164, y + 172, "Đã nhận dạng 3 sự kiện", { size: 12.5, weight: 800, fill: c.muted })}
  ${card(x + 24, y + 238, 342, 110, c.mint)}
  ${text(x + 48, y + 270, "Lập trình Mobile", { size: 16, weight: 900 })}
  ${text(x + 48, y + 296, "Thứ 4 • 09:30 - 11:30 • B203", { size: 12.5, weight: 800, fill: c.muted })}
  ${text(x + 318, y + 296, "Sửa", { size: 12, weight: 900, fill: c.red })}
  ${card(x + 24, y + 368, 342, 110, c.pink)}
  ${text(x + 48, y + 400, "Thi Cấu trúc dữ liệu", { size: 16, weight: 900 })}
  ${text(x + 48, y + 426, "Thứ 6 • 13:00 - 14:30 • A405", { size: 12.5, weight: 800, fill: c.muted })}
  ${text(x + 318, y + 426, "Sửa", { size: 12, weight: 900, fill: c.red })}
  ${card(x + 24, y + 498, 342, 110, c.yellow2)}
  ${text(x + 48, y + 530, "Nộp báo cáo UX", { size: 16, weight: 900 })}
  ${text(x + 48, y + 556, "20/05 • 22:00 • Deadline", { size: 12.5, weight: 800, fill: c.muted })}
  ${text(x + 318, y + 556, "Sửa", { size: 12, weight: 900, fill: c.red })}
  ${button(x + 32, y + 704, 326, 52, "Lưu lịch")}
`));

screens.push(phone("20. Prototype Flow", 5, 0, (x, y) => `
  ${header(x, y, "Prototype flow", "Các luồng thao tác chính", "chart")}
  ${["Onboarding","Dashboard","Thêm lịch","Chụp ảnh lịch","Xác nhận thông tin","Lưu lịch"].map((l, i) => `${card(x + 40 + (i % 2) * 170, y + 116 + Math.floor(i / 2) * 92, 140, 54, i % 2 ? c.mint : c.yellow2, { rx: 18 })}${text(x + 110 + (i % 2) * 170, y + 149 + Math.floor(i / 2) * 92, l, { size: 12, weight: 900, "text-anchor": "middle" })}`).join("")}
  ${["Dashboard","To-Do","Thêm task","Pomodoro"].map((l, i) => `${card(x + 40 + (i % 2) * 170, y + 440 + Math.floor(i / 2) * 92, 140, 54, i % 2 ? c.pink : c.blue, { rx: 18 })}${text(x + 110 + (i % 2) * 170, y + 473 + Math.floor(i / 2) * 92, l, { size: 12, weight: 900, "text-anchor": "middle" })}`).join("")}
  ${["Dashboard","Thống kê"].map((l, i) => `${card(x + 40 + i * 170, y + 672, 140, 54, i ? c.lavender : c.yellow2, { rx: 18 })}${text(x + 110 + i * 170, y + 705, l, { size: 12, weight: 900, "text-anchor": "middle" })}`).join("")}
  ${path(`M${x + 180} ${y + 143} H${x + 210}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 280} ${y + 170} V${y + 208} H${x + 180}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 180} ${y + 235} H${x + 210}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 280} ${y + 262} V${y + 300} H${x + 180}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 180} ${y + 327} H${x + 210}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 180} ${y + 467} H${x + 210}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 280} ${y + 494} V${y + 532} H${x + 180}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
  ${path(`M${x + 180} ${y + 699} H${x + 210}`, { stroke: c.ink, "stroke-width": 2, fill: "none", markerEnd: "url(#arrow)" })}
`));

const totalRows = 6;
const SVG_W = BOARD_W;
const SVG_H = 62 + totalRows * ROW_H + 80;

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${SVG_W}" height="${SVG_H}" viewBox="0 0 ${SVG_W} ${SVG_H}">
  <defs>
    <filter id="softShadow" x="-20%" y="-20%" width="140%" height="160%">
      <feDropShadow dx="0" dy="8" stdDeviation="7" flood-color="${c.shadow}" flood-opacity=".22"/>
    </filter>
    <filter id="frameShadow" x="-10%" y="-10%" width="130%" height="130%">
      <feDropShadow dx="0" dy="14" stdDeviation="18" flood-color="#B69D79" flood-opacity=".28"/>
    </filter>
    <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto" markerUnits="strokeWidth">
      <path d="M0,0 L0,6 L6,3 z" fill="${c.ink}"/>
    </marker>
    <pattern id="paperDots" width="22" height="22" patternUnits="userSpaceOnUse">
      <circle cx="3" cy="3" r="1" fill="#E7D8BF" opacity=".45"/>
    </pattern>
  </defs>
  ${rect(0, 0, SVG_W, SVG_H, c.canvas)}
  ${rect(0, 0, SVG_W, SVG_H, "url(#paperDots)", { opacity: .48 })}
  ${text(60, 34, "MANAGING YOUR STUDY SCHEDULE - mobile UI design", { size: 22, weight: 900 })}
  ${text(60, 58, "Studygram style • Android frame 390x844 • UI kit + 19 màn hình + prototype flow", { size: 13, weight: 800, fill: c.muted })}
  ${screens.join("\n")}
</svg>`;

mkdirSync(__dirname, { recursive: true });
writeFileSync(out, svg, "utf8");
console.log(out);
