sbtPlugin := true

organization := "me.lessis"

name := "sox"

version <<= sbtVersion("0.1.0-%s-SNAPSHOT" format _)
