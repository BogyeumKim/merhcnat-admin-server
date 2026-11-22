#!/usr/bin/env bash
# PostToolUse(Edit|Write): 수정된 .java 파일이 프로젝트 관례를 지키는지 검사한다.
# 위반 시 exit 2 + stderr 메시지 → Claude 가 받아서 고친다.
f=$(jq -r '.tool_response.filePath // .tool_input.file_path // empty')
[[ "$f" == *.java && -f "$f" ]] || exit 0

perl -e '
  my $file = shift;
  open my $fh, "<", $file or exit 0;
  my @lines = <$fh>; close $fh;
  (my $cls = $file) =~ s{.*/|\.java$}{}g;
  my @v;
  for my $i (0..$#lines) {
    my $l = $lines[$i]; my $n = $i + 1;
    next if $l =~ m{^\s*(//|\*|/\*)};
    push @v, "$n: LoggerFactory 수동 로거 → \@Slf4j 사용" if $l =~ /LoggerFactory\.getLogger/;
    push @v, "$n: 와일드카드 제네릭(<?>) 금지 → 구체 타입 사용" if $l =~ /<\?[\s>]/;
    push @v, "$n: HTTP 200 외 2xx 금지 (성공=200, 실패=4xx/5xx)"
      if $l =~ /HttpStatus\.(ACCEPTED|CREATED|NO_CONTENT|RESET_CONTENT|PARTIAL_CONTENT|NON_AUTHORITATIVE_INFORMATION|MULTI_STATUS)\b/
      || $l =~ /\.status\(\s*20[1-9]\s*\)/;
    # 생성자 본문이 this.x = x; 대입뿐이면 롬복 대상
    if ($l =~ /^\s*(public|protected|private)?\s*\Q$cls\E\s*\([^)]*\)?\s*$|^\s*(public|protected|private)?\s*\Q$cls\E\s*\(.*\)\s*\{\s*$/) {
      my ($j, $body, $only) = ($i, 0, 1);
      $j++ while $j <= $#lines && $lines[$j] !~ /\{\s*$/;
      for my $k ($j+1 .. $#lines) {
        last if $lines[$k] =~ /^\s*\}\s*$/;
        next if $lines[$k] =~ /^\s*$/;
        $body++;
        $only = 0 unless $lines[$k] =~ /^\s*this\.(\w+)\s*=\s*\1\s*;\s*$/;
      }
      push @v, "$n: 필드 대입뿐인 생성자 → Lombok \@RequiredArgsConstructor 사용" if $body > 0 && $only;
    }
  }
  if (@v) { print STDERR "[java-conventions] $file\n", map("  - $_\n", @v), "CLAUDE.md 의 코드 규칙에 맞게 수정하세요.\n"; exit 2 }
' "$f"
