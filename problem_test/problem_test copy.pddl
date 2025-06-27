(define (problem logistics-4-0)
(:domain logistics)
(:objects
 pos1 pos2 pos3 pos4 - place
 obj1 obj2 obj3 obj4 - physobj)

(:init (at obj1 pos1) (at obj2 pos2) (at obj3 pos3) (at obj4 pos4))

(:goal (and (at obj1 pos2) (at obj2 pos1) (at obj3 pos4) (at obj4 pos3)))
)